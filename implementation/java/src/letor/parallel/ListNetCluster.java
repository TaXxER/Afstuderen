package letor.parallel;

import letor.parallel.util.AzurePigWrapper;
import letor.parallel.util.DataSets;
import letor.parallel.util.LtrUtils;
import letor.parallel.util.Metadata;
import org.apache.pig.data.Tuple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * MapReduce (Hadoop) implementation of the ListNet algorithm
 * Makes use of AzurePigWrapper
 *
 */

public class ListNetCluster {
    // Initialise hyper-parameters
    private static final DataSets.DataSet DATASET = DataSets.DataSet.MSLR_WEB30K;
    private static final double   STEPSIZE   = 0.0001; // MSLR-WEB10K: 0.0001, ohsumed: 0.01
    private static final int      ITERATIONS = 10;
    private static final int      FOLDS      = 5;
    private static final int      k          = 10; // NDCG@k

    // Initialise paralellisation parameters
    private static int  availableMappers              = 16;
    private static int  availableReducers             = 8;

    private static Metadata metadata                  = DataSets.getMetaData(DATASET);
    private static long MAX_TRAIN_SIZE                = metadata.getMax_train_size(); // ohsumed: 5151958, MQ2007: 25820919, MQ2008: 5927007, MSLR-WEB10K: 838011150, MSLR-WEB30K:
    private static long MAX_VALI_SIZE                 = metadata.getMax_vali_size(); // ohsumed: 1764005, MQ2007:  8753466, MQ2008: 2237346, MSLR-WEB10K: 280714022, MSLR-WEB30K:
    private static long MAX_TEST_SIZE                 = metadata.getMax_test_size(); // ohsumed: 1764005, MQ2007:  8753466, MQ2008: 2237346, MSLR-WEB10K: 280714022, MSLR-WEB30K:
    private static int  DIM                           = metadata.getDim();

    private static String trainConfigString = LtrUtils.getPigConfigString(MAX_TRAIN_SIZE, availableMappers, availableReducers);
    private static String valiConfigString  = LtrUtils.getPigConfigString(MAX_VALI_SIZE, availableMappers, availableReducers);
    private static String testConfigString  = LtrUtils.getPigConfigString(MAX_TEST_SIZE, availableMappers, availableReducers);

    public static void main(String[] args) throws Exception {
        // Cluster configuration
        String clusterName          = "ltrold";
        String containerName        = "ltrmini2";
        String clusterUser          = "admin";
        String clusterPassword      = "Qw!23456789";
        String storageAccount       = "ltrstorage";
        String storageAccountKey    = "igtZD3Jih9lsvxoIcxCury1GDqS7Z4DQ0Ci7xVphY9p/6rnwaHG5qZFBKXjt0wOgwNwVqno5sitAy/eucuPGMA==";
        AzurePigWrapper apw         = new AzurePigWrapper(clusterName, containerName, clusterUser, clusterPassword, storageAccount, storageAccountKey);

        ArrayList<String> pigLines  = new ArrayList<String>();

        double[] foldNdcg = new double[FOLDS];
        double sumNdcg = 0.0;
        double averageNdcg = 0.0;
        String pathPrefix   = "/user/hdp";

        // Start timer
        Long startTime = System.nanoTime();

        for(int f=0; f<FOLDS; f++) {
            int fold = f+1;
            double[] w = new double[DIM];
            double[] gradient = new double[DIM];
            double[] bestW = new double[DIM];
            double   bestNdcg = 0.0;

            // Standardize data and scale features
            pigLines.add(trainConfigString);
            pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
            pigLines.add("TRAIN = LOAD '" + pathPrefix + "/input/" + metadata.getName() + "/Fold" + fold + "/train.txt' USING PigStorage(' ');");
            pigLines.add("TRAIN_STD = FOREACH TRAIN GENERATE flatten(udf.util.ToStandardForm($0..));");
            pigLines.add("TRAIN_STD_BY_QUERY = GROUP TRAIN_STD BY $1;");
            pigLines.add("MIN_MAX = FOREACH TRAIN_STD_BY_QUERY GENERATE flatten(udf.util.GetMinMax($0..));");
            pigLines.add("MIN_MAX_GRPD = GROUP MIN_MAX ALL;");
            pigLines.add("MIN_MAX_FIN = FOREACH MIN_MAX_GRPD GENERATE flatten(udf.util.CombineMinMax($0..));");
            pigLines.add("STORE MIN_MAX_FIN INTO 'minmax"+f+"';");
            String combinedPigLines = LtrUtils.concatenateStringList(pigLines);
            System.out.println("combinedPigLines: "+combinedPigLines);
            String minmaxString = apw.azureRunPig(combinedPigLines, "minmax"+f);
            pigLines.clear();
            String[] minmaxStrings = minmaxString.split("\\s+");
            List<Double> minmaxList = new ArrayList<Double>();
            for(String minmax : minmaxStrings)
                minmaxList.add(Double.parseDouble(minmax));
            pigLines.add(trainConfigString);
            pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
            pigLines.add("TRAIN = LOAD '" + pathPrefix + "/input/" + metadata.getName() + "/Fold" + fold + "/train.txt' USING PigStorage(' ');");
            pigLines.add("VALIDATE = LOAD '" + pathPrefix + "/input/" + metadata.getName() + "/Fold" + fold + "/vali.txt' USING PigStorage(' ');");
            pigLines.add("TEST = LOAD '" + pathPrefix + "/input/" + metadata.getName() + "/Fold" + fold + "/test.txt' USING PigStorage(' ');");
            pigLines.add("TRAIN_STD = FOREACH TRAIN GENERATE flatten(udf.util.ToStandardForm($0..));");
            pigLines.add("VALIDATE_STD = FOREACH VALIDATE GENERATE flatten(udf.util.ToStandardForm($0..));");
            pigLines.add("TEST_STD = FOREACH TEST GENERATE flatten(udf.util.ToStandardForm($0..));");
            pigLines.add("DEFINE ScaleFeatures udf.util.ScaleFeatures('"+LtrUtils.toParamString(minmaxList)+"');");
            pigLines.add("TRAIN_SCA = FOREACH TRAIN_STD GENERATE flatten(ScaleFeatures($0..));");
            pigLines.add("VALIDATE_SCA = FOREACH VALIDATE_STD GENERATE flatten(ScaleFeatures($0..));");
            pigLines.add("TEST_SCA = FOREACH TEST_STD GENERATE flatten(ScaleFeatures($0..));");
            pigLines.add("STORE TRAIN_SCA INTO 'train_sca"+f+"' USING BinStorage();");
            pigLines.add("STORE VALIDATE_SCA INTO 'validate_sca"+f+"' USING BinStorage();");
            pigLines.add("STORE TEST_SCA INTO 'test_sca"+f+"' USING BinStorage();");
            combinedPigLines = LtrUtils.concatenateStringList(pigLines);
            System.out.println("combinedPigLines: "+combinedPigLines);
            apw.azureRunPig(combinedPigLines);
            pigLines.clear();

            for (int i = 1; i <= ITERATIONS; i++) {
                // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
                // val ourScores = q.docFeatures.map(x => w dot x);
                // val expOurScores = ourScores.map(z => math.exp(z));
                // val sumExpRelScores = expRelScores.reduce(_ + _);
                // val P_y = expRelScores.map(y => y/sumExpRelScores);
                // val sumExpOurScores = expOurScores.reduce(_ + _);
                // val P_z = expOurScores.map(z => z/sumExpOurScores);
                pigLines.add(trainConfigString);
                pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
                pigLines.add("DEFINE QueryLossGradient udf.listnet.QueryLossGradient('" + DIM + "');");
                pigLines.add("DEFINE ExpRelOurScores udf.listnet.ExpRelOurScores('" + LtrUtils.toParamString(w, i) + "');");
                if (i == 1){
                    //pigLines.add("TRAIN = LOAD '" + pathPrefix + "/input/" + metadata.getName() + "/Fold" + fold + "/train.txt' USING PigStorage(' ');");
                    //pigLines.add("TRAIN_STD = FOREACH TRAIN GENERATE flatten(udf.util.ToStandardForm($0..));");
                    pigLines.add("TRAIN_SCA = LOAD 'train_sca"+f+"/*' USING BinStorage();");
                    pigLines.add("TR_BY_QUERY = GROUP TRAIN_SCA BY $1;");
                    pigLines.add("TR_EXP_REL_SCORES = FOREACH TR_BY_QUERY GENERATE flatten(ExpRelOurScores(TRAIN_SCA));");
                    pigLines.add("STORE TR_EXP_REL_SCORES INTO 'tr_exp_rel_scores-f"+f+"' USING BinStorage();");
                }else {
                    pigLines.add("TR_EXP_REL_SCORES = LOAD 'tr_exp_rel_scores-f"+f+"/*' USING BinStorage();");
                    pigLines.add("TR_EXP_REL_SCORES = FOREACH TR_EXP_REL_SCORES GENERATE flatten(ExpRelOurScores($0..));");
                }

                // UPDATE MODEL
                // var lossForAQuery = 0.0;
                // var gradientForAQuery = spark.examples.Vector.zeros(dim);
                // for (j <- 0 to q.relScores.length-1) {
                //  gradientForAQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)))
                //  lossForAQuery += -P_y(j) * math.log(P_z(j))
                // }
                pigLines.add("TR_QUERY_LOSS_GRADIENT = FOREACH TR_EXP_REL_SCORES GENERATE flatten(QueryLossGradient($0..));");

                // gradient += gradientForAQuery; loss += lossForAQuery
                pigLines.add("TR_QUERY_LOSS_GRADIENT_GRPD = GROUP TR_QUERY_LOSS_GRADIENT ALL;");
                pigLines.add("TR_LOSS_GRADIENT = FOREACH TR_QUERY_LOSS_GRADIENT_GRPD GENERATE flatten(udf.listnet.MultiSum($0..));");
                String outputDir = "tr_loss_gradient-f"+f+"i"+i;
                pigLines.add("STORE TR_LOSS_GRADIENT INTO '"+outputDir+"';");
                combinedPigLines = LtrUtils.concatenateStringList(pigLines);
                System.out.println("combinedPigLines: "+combinedPigLines);
                String lossGradientString = apw.azureRunPig(combinedPigLines, outputDir);
                pigLines.clear();
                String[] lossGradients = lossGradientString.split("\t");
                double loss = Double.parseDouble(lossGradients[0]);
                for(int j=1; j<lossGradients.length; j++)
                    gradient[j-1] = Double.parseDouble(lossGradients[j]);

                // w -= gradient.value * stepSize
                for(int j = 0; j < w.length; j++)
                    w[j] -= (gradient[j] * STEPSIZE);

                // EVALUATE MODEL ON VALIDATION SET
                pigLines.add(valiConfigString);
                pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
                pigLines.add("DEFINE Ndcg udf.util.Ndcg('" + LtrUtils.toParamString(w,k) + "');");

                if (i == 1){
                    //pigLines.add("VALIDATE = LOAD '" + pathPrefix + "/input/" + metadata.getName() + "/Fold" + fold + "/vali.txt' USING PigStorage(' ');");
                    //pigLines.add("VALIDATE_STD = FOREACH VALIDATE GENERATE flatten(udf.util.ToStandardForm($0..));");
                    pigLines.add("VALIDATE_SCA = LOAD 'validate_sca"+f+"/*' USING BinStorage();");
                    pigLines.add("VA_BY_QUERY = GROUP VALIDATE_SCA BY $1;");
                    outputDir = "va_by_query-f"+f;
                    pigLines.add("STORE VA_BY_QUERY INTO '"+outputDir+"' USING BinStorage();");
                }else {
                    pigLines.add("VA_BY_QUERY = LOAD 'va_by_query-f"+f+"/*' USING BinStorage();");
                }

                pigLines.add("NDCG = FOREACH VA_BY_QUERY GENERATE Ndcg($0..);");
                pigLines.add("NDCG_GRPD = GROUP NDCG ALL;");
                pigLines.add("AVG_NDCG = FOREACH NDCG_GRPD GENERATE AVG(NDCG);");
                outputDir = "avg_ndcg-f"+f+"i"+i;
                pigLines.add("STORE AVG_NDCG INTO '"+outputDir+"';");
                combinedPigLines = LtrUtils.concatenateStringList(pigLines);
                System.out.println("combinedPigLines: "+combinedPigLines);
                String avg_ndcg = apw.azureRunPig(combinedPigLines, outputDir);
                pigLines.clear();
                double currentNdcg = Double.parseDouble(avg_ndcg);

                if(currentNdcg > bestNdcg){
                    bestNdcg = currentNdcg;
                    bestW = w.clone();
                }

                System.out.println();
                System.out.println("Fold:         " + fold);
                System.out.println("Iteration:    " + i);
                System.out.println("loss:         " + loss);
                System.out.println("w:            " + LtrUtils.toParamString(w));
                System.out.println("bestw:        " + LtrUtils.toParamString(bestW));
                System.out.println("NDCG@"+k+":      "+currentNdcg);
                System.out.println("best NDCG@"+k+": "+bestNdcg);
                System.out.println();
            }

            pigLines.add(testConfigString);
            pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
            //pigLines.add("TEST = LOAD '" + pathPrefix + "/input/" + metadata.getName() + "/Fold" + fold + "/test.txt' USING PigStorage(' ');");
            //pigLines.add("TEST_STD = FOREACH TEST GENERATE flatten(udf.util.ToStandardForm($0..));");
            pigLines.add("TEST_SCA = LOAD 'test_sca"+f+"/*' USING BinStorage();");
            pigLines.add("TE_BY_QUERY = GROUP TEST_SCA BY $1;");

            // CALCULATE TEST SET PREDICTIONS BASED ON BEST WEIGHTS
            pigLines.add("DEFINE Ndcg udf.util.Ndcg('" + LtrUtils.toParamString(bestW,k) + "');");
            // CALCULATE NDCG@K FOR TEST SET PREDICTIONS
            pigLines.add("NDCG = FOREACH TE_BY_QUERY GENERATE Ndcg($0..);");
            pigLines.add("NDCG_GRPD = GROUP NDCG ALL;");
            pigLines.add("AVG_NDCG = FOREACH NDCG_GRPD GENERATE AVG(NDCG);");
            pigLines.add("STORE AVG_NDCG INTO 'avg_ndcg';");

            String concatenatedPigLines = LtrUtils.concatenateStringList(pigLines);
            String outputDir = "avg_ndcg";
            System.out.println("combinedPigLines: "+concatenatedPigLines);
            String avg_ndcg = apw.azureRunPig(concatenatedPigLines, outputDir);
            pigLines.clear();

            foldNdcg[f] = Double.parseDouble(avg_ndcg);
            sumNdcg += foldNdcg[f];
        }
        Long endTime = System.nanoTime();

        averageNdcg = sumNdcg / FOLDS;
        for(int i=0; i<FOLDS; i++){
            System.out.println("Fold "+(i+1)+": "+foldNdcg[i]);
        }
        System.out.println("Average: "+averageNdcg);
        System.out.println("Time: "+(endTime-startTime)/1000000+" ms");
    }
}
