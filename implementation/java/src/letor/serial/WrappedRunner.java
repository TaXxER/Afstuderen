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
        Integer                     folds               = 1;
        Integer                     iterations          = 5;
        AbstractParameterizedRanker handler             = new SmoothRankHandler();
        DataSets.DataSet            dataset             = DataSets.DataSet.OHSUMED;
        Integer                     duplicationNumber   = 1; // Only relevant in case of dataset Custom

        FoldRunHandler ltrWrapper = null;
        if(dataset == DataSets.DataSet.CUSTOM)
            ltrWrapper = new FoldRunHandler(handler, DataSets.DataSet.CUSTOM, duplicationNumber,  folds, iterations);
        else
            ltrWrapper = new FoldRunHandler(handler, dataset, folds, iterations);
        Measurement measurement = ltrWrapper.averageScore();
        System.out.println("Preprocessing: "+measurement.getPreprocessingTime()+" ms");
        System.out.println("Train:         "+measurement.getTrainingTime()+" ms");
        System.out.println("Test:          "+measurement.getTestTime()+" ms");
        System.out.println("Total time:    "+measurement.getTotalTime()+" ms");
        System.out.println("NDCG@10:       "+measurement.getEvaluationResult());
    }
}