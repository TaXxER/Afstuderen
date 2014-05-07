package letor;

import org.apache.hadoop.conf.Configuration;
import org.apache.pig.PigServer;

import java.io.IOException;

/**
 * MapReduce implementation of the ListNet algorithm
 */

public class ListNet {
    // Initialise hyper-parameters
    private static final double   BETA       = 0.1;
    private static final double   ALPHA      = 0.1;
    private static final int      ITERATIONS = 3;

    // Initialise environment settings
    // TODO: Currently hardcoded, deduce from data
    private static final int DIM        = 45;
    private static final int QUERIES    = 20;

    public static void main(String[] args) throws Exception {
        // Connect to Pig
        PigServer pigServer = new PigServer("local");
        pigServer.registerQuery("register 'hdfs://localhost:8020/pig/udf/piggybank.jar'");

        // Initialise internal model parameters
        double[] w        = new double[DIM];
        double[] gradient = new double[DIM];
        double   loss     = 0.0;

        for(int i=0;i<=ITERATIONS;i++){
            for(int q=0;q<=QUERIES;q++){
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
    }

    public static void gradientDescent(PigServer pigServer) throws IOException {
        pigServer.registerQuery("A = LOAD 'input/ohsumed/Fold1/train.txt' using PigStorage(' ');");
        pigServer.registerQuery("");
    }

}
