package letor.parallel;

import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;

import java.util.Iterator;

/**
 * MapReduce implementation of the ListNet algorithm
 */

public class ListNet {
    // Initialise hyper-parameters
    private static final double   STEPSIZE   = 0.01;
    private static final int      ITERATIONS = 100;

    public static void main(String[] args) throws Exception {
        // Connect to Pig
        PigServer pigServer = new PigServer("local");

        // Register UFFs and prepare dataset
        pigServer.registerJar("C:/Git-data/Afstuderen/implementation/java/out/artifacts/listnet_udfs_jar/java.jar");
        pigServer.registerQuery("TRAIN = LOAD 'input/ohsumed/Fold1/train.txt' USING PigStorage(' ');");
        pigServer.registerQuery("BY_QUERY = GROUP TRAIN BY $1;");

        // Set environment parameters
        int DIM         = 45;

        // Initialise internal model parameters
        double[] w        = new double[DIM];
        double[] gradient = new double[DIM];

        for(int i=1;i<=ITERATIONS;i++){
            // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
            // val ourScores = q.docFeatures.map(x => w dot x);
            // val expOurScores = ourScores.map(z => math.exp(z));
            // val sumExpRelScores = expRelScores.reduce(_ + _);
            // val P_y = expRelScores.map(y => y/sumExpRelScores);
            // val sumExpOurScores = expOurScores.reduce(_ + _);
            // val P_z = expOurScores.map(z => z/sumExpOurScores);
            pigServer.registerQuery("DEFINE ExpRelOurScores"+i+" udf.listnet.ExpRelOurScores('" + toParamString(w, i) + "');");
            if(i==1)
                pigServer.registerQuery("EXP_REL_SCORES = FOREACH BY_QUERY GENERATE flatten(ExpRelOurScores"+i+"(TRAIN));");
            else
                pigServer.registerQuery("EXP_REL_SCORES = FOREACH EXP_REL_SCORES GENERATE flatten(ExpRelOurScores"+i+"($0..));");
            // var lossForAQuery = 0.0;
            // var gradientForAQuery = spark.examples.Vector.zeros(dim);
            // for (j <- 0 to q.relScores.length-1) {
            //  gradientForAQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)))
            //  lossForAQuery += -P_y(j) * math.log(P_z(j))
            // }
            pigServer.registerQuery("QUERY_LOSS_GRADIENT = FOREACH EXP_REL_SCORES GENERATE flatten(udf.listnet.QueryLossGradient($0..));");

            // gradient += gradientForAQuery; loss += lossForAQuery
            pigServer.registerQuery("QUERY_LOSS_GRADIENT_GRPD = GROUP QUERY_LOSS_GRADIENT ALL;");
            pigServer.registerQuery("LOSS_GRADIENT = FOREACH QUERY_LOSS_GRADIENT_GRPD GENERATE flatten(udf.listnet.MultiSum($0..));");
//
            Iterator<Tuple> LOSS_GRADIENT = pigServer.openIterator("LOSS_GRADIENT");
            Tuple lossGradientTuple = LOSS_GRADIENT.next();
            double loss = Double.parseDouble(lossGradientTuple.get(0).toString());
            for(int j=1; j<lossGradientTuple.size(); j++){
                gradient[j-1] = Double.parseDouble(lossGradientTuple.get(j).toString());
            }
            System.out.println("gradient: "+toParamString(gradient));

            // w -= gradient.value * stepSize
            for(int j=0; j<w.length; j++){
                w[j] -= (gradient[j] * STEPSIZE);
            }

            System.out.println();
            System.out.println("Iteration: "+i);
            System.out.println("w:         "+toParamString(w));
            System.out.println("loss:      "+loss);
            System.out.println();
        }
    }

    private static String toParamString(double[] elems, int iteration){
        String value = "";
        for(double elem : elems)
            value += elem + ",";

        // remove last comma
        value = value.substring(0,value.length()-1);

        value += ";"+iteration;

        return value;
    }

    private static String toParamString(double[] elems){
        String value = "";
        for(double elem : elems)
            value += elem + ",";

        // remove last comma
        value = value.substring(0,value.length()-1);

        return value;
    }

}
