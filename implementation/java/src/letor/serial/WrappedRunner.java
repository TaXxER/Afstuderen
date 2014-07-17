package letor.serial;

import letor.serial.parameterizedRankers.SmoothRankHandler;
import letor.serial.util.Measurement;

/**
 * Created by niek.tax on 7/15/2014.
 */
public class WrappedRunner {
    public static void main(String[] args){
        FoldRunHandler listNetWrapper = new FoldRunHandler(new SmoothRankHandler(), "ohsumed");
        Measurement measurement = listNetWrapper.averageScore();
        System.out.println("Time elapsed: "+measurement.getRunningTime());
        System.out.println("NDCG@10:      "+measurement.getEvaluationResult()+" ms");
    }
}
