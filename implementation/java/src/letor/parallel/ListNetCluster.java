package letor.parallel;

import letor.parallel.util.AzurePigWrapper;
import letor.parallel.util.LtrUtils;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;

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

    // Run mode
    private static final ExecType runType    = ExecType.MAPREDUCE;

    public static void main(String[] args) throws Exception {
        AzurePigWrapper apw        = new AzurePigWrapper();
        double[] foldNdcg = new double[FOLDS];
        double sumNdcg = 0.0;
        double averageNdcg = 0.0;
        int DIM = 0;
        String pathPrefix   = "/user/hdp";

        // Start timer
        Long startTime = System.nanoTime();

        // Connect to Pig
        // Register Jar with UDFs
       String register = "REGISTER java.jar;";

        for(int f=0; f<FOLDS; f++) {
            int fold = f+1;
            // Load datasets
            String train = "TRAIN = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/train.txt' USING PigStorage(' ');";
            String vali  = "VALIDATE = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/vali.txt' USING PigStorage(' ');";
            String test  = "TEST = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/test.txt' USING PigStorage(' ');";

            // Transform data to standard form
            String train_std = "TRAIN_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));";
            String vali_std  = "VALIDATE_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));";
            String test_std  = "TEST_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));";

            // Group data by query
            String tr_by_query = "TR_BY_QUERY = GROUP TRAIN_STD BY $1;";
            String va_by_query = "VA_BY_QUERY = GROUP VALIDATE_STD BY $1;";
            String te_by_query = "TE_BY_QUERY = GROUP TEST_STD BY $1;";

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
            String def_query_loss_gradient = "DEFINE QueryLossGradient" + " udf.listnet.QueryLossGradient('" + DIM + "');";
            for (int i = 1; i <= ITERATIONS; i++) {
                // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
                // val ourScores = q.docFeatures.map(x => w dot x);
                // val expOurScores = ourScores.map(z => math.exp(z));
                // val sumExpRelScores = expRelScores.reduce(_ + _);
                // val P_y = expRelScores.map(y => y/sumExpRelScores);
                // val sumExpOurScores = expOurScores.reduce(_ + _);
                // val P_z = expOurScores.map(z => z/sumExpOurScores);
                String def_exp_rel_our_scores = "DEFINE ExpRelOurScores" + i + " udf.listnet.ExpRelOurScores('" + LtrUtils.toParamString(w, i) + "');";
                String tr_exp_rel_scores = "";
                if (i == 1)
                    tr_exp_rel_scores = "TR_EXP_REL_SCORES = FOREACH TR_BY_QUERY GENERATE flatten(ExpRelOurScores" + i + "(TRAIN_STD));";
                else
                    tr_exp_rel_scores = "TR_EXP_REL_SCORES = FOREACH TR_EXP_REL_SCORES GENERATE flatten(ExpRelOurScores" + i + "($0..));";

                // UPDATE MODEL
                // var lossForAQuery = 0.0;
                // var gradientForAQuery = spark.examples.Vector.zeros(dim);
                // for (j <- 0 to q.relScores.length-1) {
                //  gradientForAQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)))
                //  lossForAQuery += -P_y(j) * math.log(P_z(j))
                // }
                String tr_query_loss_gradient = "TR_QUERY_LOSS_GRADIENT = FOREACH TR_EXP_REL_SCORES GENERATE flatten(QueryLossGradient($0..));";

                // gradient += gradientForAQuery; loss += lossForAQuery
                String tr_query_loss_gradient_grpd = "TR_QUERY_LOSS_GRADIENT_GRPD = GROUP TR_QUERY_LOSS_GRADIENT ALL;";
                String tr_loss_gradient = "TR_LOSS_GRADIENT = FOREACH TR_QUERY_LOSS_GRADIENT_GRPD GENERATE flatten(udf.listnet.MultiSum($0..));";
                String outputDir = "tr_loss_gradient"+i;
                String store_tr_loss_gradient = "STORE TR_LOSS_GRADIENT INTO '"+outputDir+"'";
                String combinedPigLines = register + train + vali + test + train_std + vali_std + test_std + tr_by_query + va_by_query + te_by_query + def_query_loss_gradient + def_exp_rel_our_scores + tr_exp_rel_scores + tr_query_loss_gradient + tr_query_loss_gradient_grpd + tr_loss_gradient + store_tr_loss_gradient;
                String lossGradientTuple = apw.azureRunPig(combinedPigLines, outputDir);
                System.out.println("lossGradientTuple: "+lossGradientTuple);
                /*Iterator<Tuple> LOSS_GRADIENT = pigServer.openIterator("TR_LOSS_GRADIENT");
                Tuple lossGradientTuple = LOSS_GRADIENT.next();
                double loss = Double.parseDouble(lossGradientTuple.get(0).toString());
                for (int j = 1; j < lossGradientTuple.size(); j++) {
                    gradient[j - 1] = Double.parseDouble(lossGradientTuple.get(j).toString());
                }

                // w -= gradient.value * stepSize
                for (int j = 0; j < w.length; j++) {
                    w[j] -= (gradient[j] * STEPSIZE);
                }

                // EVALUATE MODEL ON VALIDATION SET
                pigServer.registerQuery("DEFINE Ndcg" + i + " udf.listnet.Ndcg('" + LtrUtils.toParamString(w,k) + "');");
                pigServer.registerQuery("NDCG = FOREACH VA_BY_QUERY GENERATE Ndcg"+i+"($0..);");
                pigServer.registerQuery("NDCG_GRPD = GROUP NDCG ALL;");
                pigServer.registerQuery("AVG_NDCG = FOREACH NDCG_GRPD GENERATE AVG(NDCG);");
                Iterator<Tuple> NdcgTuple = pigServer.openIterator("AVG_NDCG");
                double currentNdcg = Double.parseDouble(NdcgTuple.next().get(0).toString());
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
                System.out.println();*/
            }
            // CALCULATE TEST SET PREDICTIONS BASED ON BEST WEIGHTS
            //pigServer.registerQuery("DEFINE Ndcg" + ITERATIONS+1 + " udf.listnet.Ndcg('" + LtrUtils.toParamString(bestW,k) + "');");
            // CALCULATE NDCG@K FOR TEST SET PREDICTIONS
            //pigServer.registerQuery("NDCG = FOREACH TE_BY_QUERY GENERATE Ndcg"+ITERATIONS+1+"($0..);");
            //pigServer.registerQuery("NDCG_GRPD = GROUP NDCG ALL;");
            //pigServer.registerQuery("AVG_NDCG = FOREACH NDCG_GRPD GENERATE AVG(NDCG);");
            //Iterator<Tuple> NdcgTuple = pigServer.openIterator("AVG_NDCG");
            //foldNdcg[f] = Double.parseDouble(NdcgTuple.next().get(0).toString());
            //sumNdcg += foldNdcg[f];
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
