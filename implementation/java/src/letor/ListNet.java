package letor;

import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;

import java.util.Iterator;

/**
 * MapReduce implementation of the ListNet algorithm
 */

public class ListNet {
    // Initialise hyper-parameters
    private static final double   STEPSIZE   = 0.1;
    private static final int      ITERATIONS = 10;

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
        double   loss     = 0.0;

        for(int i=1;i<=ITERATIONS;i++){
            // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
            // val ourScores = q.docFeatures.map(x => w dot x);
            // val expOurScores = ourScores.map(z => math.exp(z));
            // val sumExpRelScores = expRelScores.reduce(_ + _);
            // val P_y = expRelScores.map(y => y/sumExpRelScores);
            // val sumExpOurScores = expOurScores.reduce(_ + _);
            // val P_z = expOurScores.map(z => z/sumExpOurScores);
            pigServer.registerQuery("DEFINE ExpRelOurScores"+i+" udf.listnet.ExpRelOurScores('" + toParamString(w, i) + "');");
            pigServer.registerQuery("EXP_REL_SCORES = FOREACH BY_QUERY GENERATE ExpRelOurScores"+i+"(TRAIN);");

//            Iterator<Tuple> EXP_REL_SCORES = pigServer.openIterator("EXP_REL_SCORES");
//            while(EXP_REL_SCORES.hasNext()){
//                System.out.println(EXP_REL_SCORES.next().getAll());
//            }

            // var lossForAQuery = 0.0;
            // var gradientForAQuery = spark.examples.Vector.zeros(dim);
            // for (j <- 0 to q.relScores.length-1) {
            //  gradientForAQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)))
            //  lossForAQuery += -P_y(j) * math.log(P_z(j))
            // }
            // gradient += gradientForAQuery; loss += lossForAQuery
            pigServer.registerQuery("QUERY_LOSS_GRADIENT = FOREACH EXP_REL_SCORES GENERATE udf.listnet.QueryLossGradient($0..);");

            Iterator<Tuple> QUERY_LOSS_GRADIENT = pigServer.openIterator("QUERY_LOSS_GRADIENT");
            while(QUERY_LOSS_GRADIENT.hasNext()){
                System.out.println(QUERY_LOSS_GRADIENT.next().getAll());
            }

            // w -= gradient.value * stepSize
            // TODO: Read variable gradient from Pig-alias QUERY_LOSS_GRADIENT
            for(int j=0; j<w.length; j++){
                w[j] -= gradient[j];
            }
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

}
