package letor.serial;

import letor.parallel.util.DataSets;
import letor.serial.parameterizedRankers.AbstractParameterizedRanker;
import letor.serial.parameterizedRankers.ListNetHandler;
import letor.serial.parameterizedRankers.SmoothRankHandler;
import letor.serial.util.Measurement;

/**
 * Created by niek.tax on 7/15/2014.
 */
public class WrappedRunner {

    public static void main(String[] args){
        AbstractParameterizedRanker handler             = new SmoothRankHandler();
        DataSets.DataSet            dataset             = DataSets.DataSet.CUSTOM;
        Integer                     duplicationNumber   = 1; // Only relevant in case of dataset Custom

        FoldRunHandler ltrWrapper = null;
        if(dataset == DataSets.DataSet.CUSTOM)
            ltrWrapper = new FoldRunHandler(handler, DataSets.DataSet.CUSTOM, duplicationNumber,  1, 1); // Method, dataset, folds, iterations
        else
            ltrWrapper = new FoldRunHandler(handler, dataset, 1, 1); // Method, dataset, folds, iterations
        Measurement measurement = ltrWrapper.averageScore();
        System.out.println("Preprocessing: "+measurement.getPreprocessingTime());
        System.out.println("Train:         "+measurement.getTrainingTime());
        System.out.println("Test:          "+measurement.getTestTime());
        System.out.println("Total time:    "+measurement.getTotalTime());
        System.out.println("NDCG@10:       "+measurement.getEvaluationResult()+" ms");
    }
}
