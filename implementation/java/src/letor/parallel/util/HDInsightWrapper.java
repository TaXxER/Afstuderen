package letor.parallel.util;

import com.json.exceptions.JSONParsingException;
import com.microsoft.windowsazure.services.blob.client.*;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Wrapper class to run pig code on Microsoft Azure
 *
 * @author Niek Tax
 */
public class HDInsightWrapper {
    String clusterName;
    String clusterUser;
    String clusterPassword;
    String storageAccount;
    String storageAccountKey;
    String containerName;
    String encoding;

    String azureClusterURL;
    String postPigURL;
    String retrieveStatusURL;

    String storageConnectionString;
    String storagePath             = "user/hdp/";
    String tempLocalStorage        = "/tmp/";

    HttpClient clusterConnection;

    int availableReducers;

    public HDInsightWrapper(String clusterName, String containerName, String clusterUser, String clusterPassword, String storageAccount, String storageAccountKey, int availableReducers){
        this.clusterName        = clusterName;
        this.containerName      = containerName;
        this.clusterUser        = clusterUser;
        this.clusterPassword    = clusterPassword;
        this.storageAccount     = storageAccount;
        this.storageAccountKey  = storageAccountKey;
        this.availableReducers  = availableReducers;

        this.storageConnectionString =
                "DefaultEndpointsProtocol=http;" +
                "AccountName="+storageAccount+";"+
                "AccountKey="+storageAccountKey;

        this.clusterConnection   = HttpClientBuilder.create().build();
        this.encoding            = new String( Base64.encodeBase64((clusterUser + ":" + clusterPassword).getBytes()) );
        this.azureClusterURL     = "https://"+clusterName+".azurehdinsight.net:443/templeton/v1";
        this.postPigURL          = azureClusterURL+"/pig";
        this.retrieveStatusURL   = azureClusterURL+"/queue/";
    }

    public HDInsightWrapper(String clusterName, String clusterUser, String clusterPassword, String storageAccount, String storageAccountKey, int availableReducers){
        this(clusterName, clusterName, clusterUser, clusterPassword, storageAccount, storageAccountKey, availableReducers);
    }

    public void runPig(String pigLine) throws Exception{
        // STEP 1: Post pig job
        HttpResponse response = null;
        int          attempt  = 0;
        while (response==null && attempt<3) {
            HttpPost postRequest = new HttpPost(postPigURL);
            postRequest.setHeader("Authorization", "Basic " + encoding);

            List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
            postParameters.add(new BasicNameValuePair("execute", pigLine));
            postParameters.add(new BasicNameValuePair("user.name", "hdp"));

            postRequest.addHeader("execute", pigLine);
            postRequest.setEntity(new UrlEncodedFormEntity(postParameters));
            try {
                response = clusterConnection.execute(postRequest);
            }catch (HttpHostConnectException e){
                response = null;
                attempt++;
            }
        }

        String concat = "";
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        String output;

        while ((output = br.readLine()) != null) {
            concat += output + "\n\r";
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> jsonMap = objectMapper.readValue(concat, HashMap.class);
        String jobId = jsonMap.get("id");

        System.out.println("Pig Job submitted");

        // STEP 2: Poll server until finished
        boolean jobComplete = false;
        Map<String, String> lastJsonMap = null;
        while(!jobComplete) {
            final HttpGet getRequest = new HttpGet(retrieveStatusURL + jobId + "?user.name=hdp");
            getRequest.setHeader("Authorization", "Basic " + encoding);

            ExecutorService executor = Executors.newCachedThreadPool();
            Callable<HttpResponse> task = new Callable<HttpResponse>() {
                @Override
                public HttpResponse call() throws Exception {
                    return clusterConnection.execute(getRequest);
                }
            };
            Future<HttpResponse> future = executor.submit(task);
            try {
                response = future.get(12, TimeUnit.SECONDS);
            }catch (TimeoutException e){
                System.err.println("Time out, no response obtained from cluster");
                Thread.sleep(1000);
                continue;
            }

            concat = "";
            br = new BufferedReader(
                    new InputStreamReader((response.getEntity().getContent())));

            output = null;
            while ((output = br.readLine()) != null)
                concat += output + "\n\r";
            try {
                jsonMap = objectMapper.readValue(concat, HashMap.class);
            }catch(JSONParsingException e){
                System.err.println("Unable to parse server JSON response");
                continue;
            }

            if(lastJsonMap==null || !jsonMap.equals(lastJsonMap))
                System.out.println("jsonMap: "+jsonMap);
            lastJsonMap = jsonMap;

            String completed = jsonMap.get("completed");
            if(completed!=null && completed.equals("done"))
                jobComplete = true;

            Thread.sleep(50);
        }
    }

    public String runPig(String pigLine, String outputDir) throws Exception{
        //STEP 1 & 2: Post pig job and poll server until job finished
        runPig(pigLine);

        System.out.println("M/R job done, starting data retrieval");

        //STEP 3: Retrieve answer
        return retrieveData(outputDir);
    }

    private String retrieveData(String fileDir) throws Exception{
        boolean succeeded = false;
        int     attempts = 0;
        String  answer = null;
        while(!succeeded && attempts < 3) {
            try {
                answer = retrieveDataAttempt(fileDir);
                succeeded = true;
            } catch (StorageException e) {
                System.err.println("WARNING: StorageException thrown");
                attempts++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
            } catch (NullPointerException e) {
                System.err.println("WARNING: Tried to read non-existing blob");
            } catch (Exception e){
                System.err.println("WARNING: An unknown error occurred");
                e.printStackTrace();
                attempts++;
            }
        }

        if(answer==null)
            throw new Exception("Error reading data, not able to read file "+fileDir);

        return answer;
    }

    private String retrieveDataAttempt(String fileDir) throws Exception {
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobContainer container = storageAccount.createCloudBlobClient().getContainerReference(containerName);
        String retVal = "";
        for(int i=0; i<availableReducers; i++) {
            String path = "";
            if(i<10)
                path = storagePath + fileDir + "/part-r-0000"+i;
            else
                path = storagePath + fileDir + "/part-r-000"+i;

            CloudBlob blob = container.getBlockBlobReference(path);
            blob.download(new FileOutputStream(tempLocalStorage + blob.hashCode()));
            FileInputStream fis = new FileInputStream(tempLocalStorage + blob.hashCode());
            int content;
            retVal = "";
            while ((content = fis.read()) != -1)
                retVal += (char) content;
            if(!retVal.isEmpty())
                break;
        }
        return retVal;
    }
}
