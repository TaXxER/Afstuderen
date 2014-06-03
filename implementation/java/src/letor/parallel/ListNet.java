package letor.parallel;

import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;

import java.util.Iterator;

/**
 * MapReduce (Hadoop) implementation of the ListNet algorithm
 */

public class ListNet {
    // Initialise hyper-parameters
    private static final String   DATASET    = "ohsumed";
    private static final double   STEPSIZE   = 0.05;
    private static final int      ITERATIONS = 2;
    private static final int      FOLDS      = 5;
    // NDCG@k
    private static final int      k          = 10;
    ;

    public static void main(String[] args) throws Exception {
        double[] foldNdcg = new double[FOLDS];
        double sumNdcg = 0.0;
        double averageNdcg = 0.0;
        int DIM = 0;

        Long startTime = System.nanoTime();
        for(int f=0; f<FOLDS; f++) {
            int fold = f+1;
            // Connect to Pig
            PigServer pigServer = new PigServer("local");

            // Register UFFs and load and prepare dataset
            pigServer.registerJar("C:/Git-data/Afstuderen/implementation/java/out/artifacts/listnet_udfs_jar/java.jar");
            pigServer.registerQuery("TRAIN = LOAD 'input/"+DATASET+"/Fold"+fold+"/train.txt' USING PigStorage(' ');");
            pigServer.registerQuery("TRAIN_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));");
            pigServer.registerQuery("VALIDATE = LOAD 'input/"+DATASET+"/Fold"+fold+"/vali.txt' USING PigStorage(' ');");
            pigServer.registerQuery("VALIDATE_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));");
            pigServer.registerQuery("TEST = LOAD 'input/"+DATASET+"/Fold"+fold+"/test.txt' USING PigStorage(' ');");
            pigServer.registerQuery("TEST_STD = FOREACH TRAIN GENERATE flatten(udf.listnet.ToStandardForm($0..));");

            // Group data by query
            pigServer.registerQuery("TR_BY_QUERY = GROUP TRAIN_STD BY $1;");
            pigServer.registerQuery("VA_BY_QUERY = GROUP VALIDATE_STD BY $1;");
            pigServer.registerQuery("TE_BY_QUERY = GROUP TEST_STD BY $1;");

            // Determine attribute dimension
            if(fold==1){
                Iterator<Tuple> TRAIN_STD = pigServer.openIterator("TRAIN_STD");
                Tuple tup = TRAIN_STD.next();
                DIM = tup.size()-2;
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
                pigServer.registerQuery("DEFINE ExpRelOurScores" + i + " udf.listnet.ExpRelOurScores('" + toParamString(w, i) + "');");
                if (i == 1)
                    pigServer.registerQuery("TR_EXP_REL_SCORES = FOREACH TR_BY_QUERY GENERATE flatten(ExpRelOurScores" + i + "(TRAIN_STD));");
                else
                    pigServer.registerQuery("TR_EXP_REL_SCORES = FOREACH TR_EXP_REL_SCORES GENERATE flatten(ExpRelOurScores" + i + "($0..));");

                // UPDATE MODEL
                // var lossForAQuery = 0.0;
                // var gradientForAQuery = spark.examples.Vector.zeros(dim);
                // for (j <- 0 to q.relScores.length-1) {
                //  gradientForAQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)))
                //  lossForAQuery += -P_y(j) * math.log(P_z(j))
                // }
                pigServer.registerQuery("TR_QUERY_LOSS_GRADIENT = FOREACH TR_EXP_REL_SCORES GENERATE flatten(udf.listnet.QueryLossGradient($0..));");

                // gradient += gradientForAQuery; loss += lossForAQuery
                pigServer.registerQuery("TR_QUERY_LOSS_GRADIENT_GRPD = GROUP TR_QUERY_LOSS_GRADIENT ALL;");
                pigServer.registerQuery("TR_LOSS_GRADIENT = FOREACH TR_QUERY_LOSS_GRADIENT_GRPD GENERATE flatten(udf.listnet.MultiSum($0..));");
//
                Iterator<Tuple> LOSS_GRADIENT = pigServer.openIterator("TR_LOSS_GRADIENT");
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
                pigServer.registerQuery("DEFINE Ndcg" + i + " udf.listnet.Ndcg('" + toParamString(w,k) + "');");
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
                System.out.println("w:            " + toParamString(w));
                System.out.println("bestw:        " + toParamString(bestW));
                System.out.println("NDCG@"+k+":      "+currentNdcg);
                System.out.println("best NDCG@"+k+": "+bestNdcg);
                System.out.println();
            }
            // CALCULATE TEST SET PREDICTIONS BASED ON BEST WEIGHTS
            pigServer.registerQuery("DEFINE Ndcg" + ITERATIONS+1 + " udf.listnet.Ndcg('" + toParamString(bestW,k) + "');");
            // CALCULATE NDCG@K FOR TEST SET PREDICTIONS
            pigServer.registerQuery("NDCG = FOREACH TE_BY_QUERY GENERATE Ndcg"+ITERATIONS+1+"($0..);");
            pigServer.registerQuery("NDCG_GRPD = GROUP NDCG ALL;");
            pigServer.registerQuery("AVG_NDCG = FOREACH NDCG_GRPD GENERATE AVG(NDCG);");
            Iterator<Tuple> NdcgTuple = pigServer.openIterator("AVG_NDCG");
            foldNdcg[f] = Double.parseDouble(NdcgTuple.next().get(0).toString());
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

    // intValue used for both loop-counter and NDCG@k k-value
    private static String toParamString(double[] elems, int intValue){
        String value = "";
        for(double elem : elems)
            value += elem + ",";

        // remove last comma
        value = value.substring(0,value.length()-1);

        value += ";"+intValue;

        return value;
    }

    public static String toParamString(double[] elems){
        String value = "";
        for(double elem : elems)
            value += elem + ",";

        // remove last comma
        value = value.substring(0,value.length()-1);

        return value;
    }
}
