package ciir.umass.edu.experimenter;

import ciir.umass.edu.experimenter.util.DataSets;
import ciir.umass.edu.experimenter.parameterizedRankers.AbstractParameterizedRanker;
import ciir.umass.edu.experimenter.parameterizedRankers.ListNetHandler;
import ciir.umass.edu.experimenter.parameterizedRankers.SmoothRankHandler;
import ciir.umass.edu.experimenter.util.Measurement;

/**
 * Created by niek.tax on 7/15/2014.
 */
public class NormalizationExperimenter {

    public static void main(String[] args){
        Integer                     folds               = 5;
        Integer                     iterations          = 1000;
        Integer                     k                   = 10;
        AbstractParameterizedRanker handler             = new ListNetHandler();
        DataSets.DataSet            dataset             = DataSets.DataSet.OHSUMED;
        Integer                     duplicationNumber   = 1; // Only relevant in case of dataset Custom

        System.out.println("NDCG@"+k+":       "+measurement.getEvaluationResult());
    }
}