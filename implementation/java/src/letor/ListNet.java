package letor;

import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.Iterator;

/**
 * MapReduce implementation of the ListNet algorithm
 */

public class ListNet {
    // Initialise hyper-parameters
    private static final double   BETA       = 0.1;
    private static final double   ALPHA      = 0.1;
    private static final int      ITERATIONS = 3;

    public static void main(String[] args) throws Exception {
        // Connect to Pig
        PigServer pigServer = new PigServer("local");

        // Register UFFs and prepare dataset
        pigServer.registerQuery("REGISTER 'listnet-udfs.jar'");
        pigServer.registerQuery("TRAIN = LOAD 'input/ohsumed/Fold1/train.txt' USING PigStorage(' ');");
        pigServer.registerQuery("BY_QUERY = GROUP TRAIN BY $1;");

        // Obtain iterators with environment settings
        pigServer.registerQuery("BY_QUERY_GRPD = GROUP BY_QUERY ALL;");
        pigServer.registerQuery("NUM_QUERIES = FOREACH BY_QUERY_GRPD GENERATE COUNT(BY_QUERY);");
        Iterator<Tuple> NUM_QUERIES = pigServer.openIterator("NUM_QUERIES");
        int QUERIES     = (Integer) NUM_QUERIES.next().get(0);
        int DIM         = 45;

        // Initialise internal model parameters
        double[] w        = new double[DIM];
        double[] gradient = new double[DIM];
        double   loss     = 0.0;

        for(int i=0;i<=ITERATIONS;i++){
            pigServer.registerQuery("DEFINE CommaSplitter udf.listnet.ExpOurScores('"+w+"');");
            pigServer.registerQuery("EXP_OUR_SCORES = ExpOurScores(BY_QUERY);");
            /*
            Dit stuk kan bijna 1-op-1 over naar Pig Latin:
            ExpRelScores:
                val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))

            ExpOurScores:
                val ourScores = q.docFeatures.map(x => w dot x);
                val expOurScores = ourScores.map(z => math.exp(z));


            val sumExpRelScores = expRelScores.reduce(_ + _);
            val sumExpOurScores = expOurScores.reduce(_ + _);
            val P_y = expRelScores.map(y => y/sumExpRelScores);
            val P_z = expOurScores.map(z => z/sumExpOurScores);
            var lossForAQuery = 0.0;
            var gradientForAQuery = spark.examples.Vector.zeros(dim);
            */


           /* for (j <- 0 to q.relScores.length-1) {
                gradientForAQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)))
                lossForAQuery += -P_y(j) * math.log(P_z(j))
            }
            gradient += gradientForAQuery; loss += lossForAQuery
            */

        }
    }

    public static double[] gradientDescentStep(PigServer pigServer, double[] w) throws IOException {

        pigServer.registerQuery("");
        return w;
    }

}
