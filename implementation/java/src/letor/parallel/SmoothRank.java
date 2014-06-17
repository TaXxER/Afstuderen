package letor.parallel;

import letor.parallel.util.LtrUtils;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * MapReduce (Hadoop) implementation of the SmoothRank algorithm
 *
 * Created by niek.tax on 6/6/2014.
 */
public class SmoothRank {
    // Initialise hyper-parameters
    private static final String   DATASET            = "ohsumed";
    private static final double   START_SMOOTHING_FACTOR   = 64.0;
    private static final int      ITERATIONS         = 1500;
    private static final int      FOLDS              = 5;
    private static final int      k                  = 10; // NDCG@k

    // Run mode
    private static final ExecType runType            = ExecType.LOCAL;

    public static void main(String[] args) throws Exception {
        double[] foldNdcg = new double[FOLDS];
        double sumNdcg = 0.0;
        double averageNdcg = 0.0;
        int DIM = 0;
        String pathPrefix = "";

        // Start timer
        Long startTime = System.nanoTime();

        // Connect to Pig
        PigServer pigServer = null;
        if(runType==ExecType.MAPREDUCE) {
            pathPrefix = "../NiekTax";
            Properties props = new Properties();
            props.setProperty("fs.default.name", "asv://ltrmini@ltrstorage.blob.core.windows.net");
            props.setProperty("dfs.namenode.rpc-address", "hdfs://ltrmini.azurehdinsight:9000");
            props.setProperty("fs.azure.account.key.ltrstorage.blob.core.windows.net", "Ygu4G/VYx8MJLlZosMcF7nIe5WI5kxXdWfvmky+inxTn1W7hKtrRAmeAVEgh30caAoArjQHTLbF/mPRIORqpqw==");
            props.setProperty("mapred.job.tracker", "ltrmini.azurehdinsight.net:9010");
            pigServer = new PigServer(ExecType.MAPREDUCE, props);
        }else{
            pathPrefix = "C:/Git-data/Afstuderen/implementation/java";
            pigServer = new PigServer(ExecType.LOCAL);
        }

        // Register Jar with UDFs
        pigServer.registerJar("C:/Git-data/Afstuderen/implementation/java/out/artifacts/udfs_jar/java.jar");

        for(int f=0; f<FOLDS; f++) {
            int fold = f + 1;
            // Load datasets
            pigServer.registerQuery("TRAIN = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/train.txt' USING PigStorage(' ');");
            pigServer.registerQuery("VALIDATE = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/vali.txt' USING PigStorage(' ');");
            pigServer.registerQuery("TEST = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/test.txt' USING PigStorage(' ');");

            // Transform data to standard form
            pigServer.registerQuery("TRAIN_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));");
            pigServer.registerQuery("VALIDATE_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));");
            pigServer.registerQuery("TEST_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));");

            // Group data by query
            pigServer.registerQuery("TR_BY_QUERY = GROUP TRAIN_STD BY $1;");
            pigServer.registerQuery("VA_BY_QUERY = GROUP VALIDATE_STD BY $1;");
            pigServer.registerQuery("TE_BY_QUERY = GROUP TEST_STD BY $1;");

            // Determine attribute dimension
            if(fold==1){
                Iterator<Tuple> TRAIN_STD = pigServer.openIterator("TRAIN_STD");
                DIM = TRAIN_STD.next().size()-2;
            }

            // Initialise internal model parameters
            double[] w = new double[DIM];
            double[] gradient = new double[DIM];
            double[] bestW = new double[DIM];
            double   bestNdcg = 0.0;

            // Calculate w_0 with Linear Regression
            pigServer.registerQuery("QUERY_LIN_REG = FOREACH TR_BY_QUERY GENERATE flatten(udf.smoothrank.LinearRegression(TRAIN_STD));");
            pigServer.registerQuery("GRPD_LIN_REG = GROUP QUERY_LIN_REG ALL;");
            pigServer.registerQuery("AVG_LIN_REG = FOREACH GRPD_LIN_REG GENERATE udf.smoothrank.MultiAvg(QUERY_LIN_REG);");
            Iterator<Tuple> AVG_LIN_REG = pigServer.openIterator("AVG_LIN_REG");
            List wList = AVG_LIN_REG.next().getAll();
            for(int i=0; i<wList.size(); i++){
                w[i] = Double.parseDouble(wList.get(i+1).toString());
            }

            // SmoothRank iterations
            for (int i = 1; i <= ITERATIONS; i++) {
                double smoothing_factor = START_SMOOTHING_FACTOR / (2^(i-1));
                // Calculate Aq(f, sig) = sum 1,j <- 1 to m : G(rel_i) * D(j) * h_{i,j}
                pigServer.registerQuery("DEFINE ExpRelOurScores" + i + " udf.smoothrank.ExpRelOurScores('" + LtrUtils.toParamString(w, i) + "');");
                if (i == 1)
                    pigServer.registerQuery("TR_EXP_REL_SCORES = FOREACH TR_BY_QUERY GENERATE flatten(ExpRelOurScores" + i + "(TRAIN_STD));");
                else
                    pigServer.registerQuery("TR_EXP_REL_SCORES = FOREACH TR_EXP_REL_SCORES GENERATE flatten(ExpRelOurScores" + i + "($0..));");
            }
        }

    }
}
