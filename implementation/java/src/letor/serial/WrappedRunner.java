package letor.serial;

import letor.parallel.util.DataSets;
import letor.serial.parameterizedRankers.ListNetHandler;
import letor.serial.parameterizedRankers.SmoothRankHandler;
import letor.serial.util.Measurement;

/**
 * Created by niek.tax on 7/15/2014.
 */
public class WrappedRunner {
    public static void main(String[] args){
        FoldRunHandler listNetWrapper = new FoldRunHandler(new ListNetHandler(), DataSets.DataSet.MSLR_WEB30K, 1, 1); // Method, dataset, folds, iterations
        Measurement measurement = listNetWrapper.averageScore();
        System.out.println("Preprocessing: "+measurement.getPreprocessingTime());
        System.out.println("Train:         "+measurement.getTrainingTime());
        System.out.println("Test:          "+measurement.getTestTime());
        System.out.println("Total time:    "+measurement.getTotalTime());
        System.out.println("NDCG@10:       "+measurement.getEvaluationResult()+" ms");
    }
}
