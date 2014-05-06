package letor;

/**
 * MapReduce implementation of the ListNet algorithm
 */
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.pig.PigServer;

public class ListNet {
    private static final int ITERATIONS = 3;
    private static final int DIM        = 45;
    private static final int QUERIES    = 20;

    public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            StringTokenizer tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                word.set(tokenizer.nextToken());
                context.write(word, one);
            }
        }
    }

    public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static void main(String[] args) throws Exception {
        // Connect to Pig
        PigServer pigServer = new PigServer("local");
        pigServer.registerQuery("register 'hdfs://localhost:8020/pig/udf/piggybank.jar'");

        // Initialise variables
        double[] gradient = new double[DIM];
        double   loss     = 0.0;
        double   beta     = 0.1;

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
        Configuration conf = new Configuration();

        Job job = new Job(conf, "listnet");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.waitForCompletion(true);
    }

    public static void gradientDescent(PigServer pigServer) throws IOException{
        pigServer.registerQuery("A = LOAD 'input/ohsumed/Fold1/train.txt' using PigStorage(' ');");
        pigServer.registerQuery("");
    }

}
