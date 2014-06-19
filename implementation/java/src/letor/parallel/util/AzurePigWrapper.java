package letor.parallel.util;

import com.microsoft.windowsazure.services.blob.client.*;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.*;

/**
 * Wrapper class to run pig code on Microsoft Azure
 *
 * @author Niek Tax
 */
public class AzurePigWrapper {
    String clusterName;
    String clusterUser;
    String clusterPassword;
    String storageAccount;
    String storageAccountKey;
    String containerName;

    String storageConnectionString;

    static String storagePath   = "user/hdp/";

    public AzurePigWrapper(String clusterName, String clusterUser, String clusterPassword, String storageAccount, String storageAccountKey){
        this.clusterName        = clusterName;
        this.containerName      = clusterName;
        this.clusterUser        = clusterUser;
        this.clusterPassword    = clusterPassword;
        this.storageAccount     = storageAccount;
        this.storageAccountKey  = storageAccountKey;

        this.storageConnectionString =
        "DefaultEndpointsProtocol=http;" +
        "AccountName="+storageAccount+";"+
        "AccountKey="+storageAccountKey;
    }

    public String azureRunPig(String pigLine, String tmpDir) throws Exception{
        // Configure connection variables
        String azureClusterURL         = "https://"+clusterName+".azurehdinsight.net:443/templeton/v1";
        String PostPigURL              = azureClusterURL+"/pig";
        String retrieveStatusURL       = azureClusterURL+"/queue/";


        String tempLocalStorage        = "/tmp/";

        String encoding                = new String( Base64.encodeBase64((clusterUser + ":" + clusterPassword).getBytes()) );
        HttpClient clusterConnection   = HttpClientBuilder.create().build();

        // STEP 1: Post pig job
        HttpPost postRequest = new HttpPost(PostPigURL);
        postRequest.setHeader("Authorization", "Basic " + encoding);

        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("execute", pigLine));
        postParameters.add(new BasicNameValuePair("user.name", "hdp"));

        postRequest.addHeader("execute", pigLine);

        postRequest.setEntity(new UrlEncodedFormEntity(postParameters));

        HttpResponse response = clusterConnection.execute(postRequest);

        String concat = "";
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        String output;

        while ((output = br.readLine()) != null) {
            concat += output + "\n\r";
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> jsonMap = objectMapper.readValue(concat, HashMap.class);
        String jobid = jsonMap.get("id");

        // STEP 2: Poll server until finished
        boolean jobComplete = false;
        while(!jobComplete) {
            HttpGet getRequest = new HttpGet(retrieveStatusURL + jobid);
            getRequest.setHeader("Authorization", "Basic " + encoding);

            response = clusterConnection.execute(getRequest);

            concat = "";
            br = new BufferedReader(
                    new InputStreamReader((response.getEntity().getContent())));

            output = null;
            while ((output = br.readLine()) != null)
                concat += output + "\n\r";

            jsonMap = objectMapper.readValue(concat, HashMap.class);

            String completed = jsonMap.get("completed");
            if(completed!=null && completed.equals("done"))
                jobComplete = true;

                Thread.sleep(100);
        }

        //STEP 3: Retrieve answer
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

        CloudBlobContainer container = storageAccount.createCloudBlobClient().getContainerReference(containerName);
        CloudBlob blob = container.getBlockBlobReference(storagePath + tmpDir + "/part-r-00000");
        blob.download(new FileOutputStream(tempLocalStorage+blob.hashCode()));
        FileInputStream fis = new FileInputStream(tempLocalStorage+blob.hashCode());
        int content;
        String retVal = "";
        while((content = fis.read()) != -1)
            retVal += (char) content;
        return retVal;
    }

    public void deleteFile(String path) throws Exception{
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobContainer container = storageAccount.createCloudBlobClient().getContainerReference(containerName);

        container.getBlockBlobReference(storagePath + path).delete();
        container.getBlockBlobReference(storagePath + path + "/part-r-00000").delete();
    }
}
