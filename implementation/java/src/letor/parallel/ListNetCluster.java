package letor.parallel;

import letor.parallel.util.AzurePigWrapper;
import letor.parallel.util.LtrUtils;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * MapReduce (Hadoop) implementation of the ListNet algorithm
 * Makes use of AzurePigWrapper
 *
 */

public class ListNetCluster {
    // Initialise hyper-parameters
    private static final String   DATASET    = "ohsumed";
    private static final double   STEPSIZE   = 0.00001;
    private static final int      ITERATIONS = 1500;
    private static final int      FOLDS      = 5;
    private static final int      k          = 10; // NDCG@k

    public static void main(String[] args) throws Exception {
        // Cluster configuration
        String clusterName          = "ltrmini2";
        String clusterUser          = "admin";
        String clusterPassword      = "Qw!23456789";
        String storageAccount       = "ltrstorage";
        String storageAccountKey    = "igtZD3Jih9lsvxoIcxCury1GDqS7Z4DQ0Ci7xVphY9p/6rnwaHG5qZFBKXjt0wOgwNwVqno5sitAy/eucuPGMA==";
        AzurePigWrapper apw         = new AzurePigWrapper(clusterName, clusterUser, clusterPassword, storageAccount, storageAccountKey);

        ArrayList<String> pigLines  = new ArrayList<String>();

        double[] foldNdcg = new double[FOLDS];
        double sumNdcg = 0.0;
        double averageNdcg = 0.0;
        int DIM = 0;
        String pathPrefix   = "/user/hdp";

        // Start timer
        Long startTime = System.nanoTime();

        // Connect to Pig
        // Register Jar with UDFs


        for(int f=0; f<FOLDS; f++) {
            int fold = f+1;
            pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
            // Load datasets
            pigLines.add("TRAIN = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/train.txt' USING PigStorage(' ');");
            // Transform data to standard form
            pigLines.add("TRAIN_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));");
            // Group data by query
            pigLines.add("TR_BY_QUERY = GROUP TRAIN_STD BY $1;");
            // Determine attribute dimension
            if(fold==1){
                //Iterator<Tuple> TRAIN_STD = pigServer.openIterator("TRAIN_STD");
                //DIM = TRAIN_STD.next().size()-2;
                DIM = 45;
            }

            // Initialise internal model parameters
            double[] w = new double[DIM];
            double[] gradient = new double[DIM];
            double[] bestW = new double[DIM];
            double   bestNdcg = 0.0;

            for (int i = 1; i <= ITERATIONS; i++) {
                // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
                // val ourScores = q.docFeatures.map(x => w dot x);
                // val expOurScores = ourScores.map(z => math.exp(z));
                // val sumExpRelScores = expRelScores.reduce(_ + _);
                // val P_y = expRelScores.map(y => y/sumExpRelScores);
                // val sumExpOurScores = expOurScores.reduce(_ + _);
                // val P_z = expOurScores.map(z => z/sumExpOurScores);
                pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
                pigLines.add("DEFINE QueryLossGradient" + " udf.listnet.QueryLossGradient('" + DIM + "');");
                pigLines.add("DEFINE ExpRelOurScores" + i + " udf.listnet.ExpRelOurScores('" + LtrUtils.toParamString(w, i) + "');");
                if (i == 1)
                    pigLines.add("TR_EXP_REL_SCORES = FOREACH TR_BY_QUERY GENERATE flatten(ExpRelOurScores" + i + "(TRAIN_STD));");
                else {
                    System.out.println("ITERATIE "+i);
                    pigLines.add("TR_EXP_REL_SCORES = LOAD 'tr_exp_rel_scores"+(i-1)+"' USING BinStorage();");
                    // clear tr_exp_rel_scores on blob storage
                    pigLines.add("TR_EXP_REL_SCORES = FOREACH TR_EXP_REL_SCORES GENERATE flatten(ExpRelOurScores" + i + "($0..));");
                }
                pigLines.add("STORE TR_EXP_REL_SCORES INTO 'tr_exp_rel_scores"+i+"' USING BinStorage();");

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
                String outputDir = "tr_loss_gradient"+i;
                pigLines.add("STORE TR_LOSS_GRADIENT INTO '"+outputDir+"';");
                String concatenatedPigLines = LtrUtils.concatenateStringList(pigLines);
                System.out.println("combinedPigLines: "+concatenatedPigLines);
                String lossGradientString = apw.azureRunPig(concatenatedPigLines, outputDir);
                pigLines.clear();
                if(i!=1)
                    apw.deleteFile("tr_exp_rel_scores"+(i-1));
                System.out.println("lossGradientTuple: "+lossGradientString);
                String[] lossGradients = lossGradientString.split("\t");
                double loss = Double.parseDouble(lossGradients[0]);
                double[] gradients = new double[DIM];
                for(int j=1; j<lossGradients.length; j++){
                    gradients[j-1] = Double.parseDouble(lossGradients[j]);
                }
                // w -= gradient.value * stepSize
                for(int j = 0; j < w.length; j++) {
                    w[j] -= (gradient[j] * STEPSIZE);
                }

                // EVALUATE MODEL ON VALIDATION SET
                pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
                pigLines.add("DEFINE Ndcg" + i + " udf.listnet.Ndcg('" + LtrUtils.toParamString(w,k) + "');");
                pigLines.add("VALIDATE = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/vali.txt' USING PigStorage(' ');");
                pigLines.add("VALIDATE_STD = FOREACH VALIDATE GENERATE flatten(udf.listnet.ToStandardForm($0..));");
                pigLines.add("VA_BY_QUERY = GROUP VALIDATE_STD BY $1;");
                pigLines.add("NDCG = FOREACH VA_BY_QUERY GENERATE Ndcg"+i+"($0..);");
                pigLines.add("NDCG_GRPD = GROUP NDCG ALL;");
                pigLines.add("AVG_NDCG = FOREACH NDCG_GRPD GENERATE AVG(NDCG);");
                pigLines.add("STORE AVG_NDCG INTO 'avg_ndcg';");
                concatenatedPigLines = LtrUtils.concatenateStringList(pigLines);
                outputDir = "avg_ndcg";
                System.out.println("combinedPigLines: "+concatenatedPigLines);
                String avg_ndcg = apw.azureRunPig(concatenatedPigLines, outputDir);
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

            pigLines.add("REGISTER wasb:///user/hdp/lib/listnet_udfs_jar/*.jar;");
            pigLines.add("TEST = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/test.txt' USING PigStorage(' ');");
            pigLines.add("TEST_STD = FOREACH TEST GENERATE flatten(udf.listnet.ToStandardForm($0..));");
            pigLines.add("TE_BY_QUERY = GROUP TEST_STD BY $1;");

            // CALCULATE TEST SET PREDICTIONS BASED ON BEST WEIGHTS
            pigLines.add("DEFINE Ndcg" + ITERATIONS+1 + " udf.listnet.Ndcg('" + LtrUtils.toParamString(bestW,k) + "');");
            // CALCULATE NDCG@K FOR TEST SET PREDICTIONS
            pigLines.add("NDCG = FOREACH TE_BY_QUERY GENERATE Ndcg"+ITERATIONS+1+"($0..);");
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
