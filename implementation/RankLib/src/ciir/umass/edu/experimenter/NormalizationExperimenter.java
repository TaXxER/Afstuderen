package ciir.umass.edu.experimenter;

import ciir.umass.edu.experimenter.util.DataSets;
import ciir.umass.edu.experimenter.parameterizedRankers.AbstractParameterizedRanker;
import ciir.umass.edu.experimenter.parameterizedRankers.ListNetHandler;
import ciir.umass.edu.experimenter.parameterizedRankers.SmoothRankHandler;
import ciir.umass.edu.experimenter.util.Measurement;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by niek.tax on 7/15/2014.
 */
public class NormalizationExperimenter {

    public static void main(String[] args){
        Integer                     folds               = 5;
        Integer                     maxIterations       = 1000;
        Integer                     k                   = 10;
        AbstractParameterizedRanker handler             = new ListNetHandler();

        Set<DataSets.DataSet>       datasets            = new HashSet<DataSets.DataSet>();
        // Add all LETOR 3.0 data sets
        //datasets.add(DataSets.DataSet.MSLR_WEB30K);
        datasets.add(DataSets.DataSet.MSLR_WEB10K);

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter("normalization_measurements_mslr_web.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        CSVWriter writer = new CSVWriter(out);

        for (int iterations = 1; iterations < maxIterations; iterations++) {
            for(DataSets.DataSet dataset: datasets) {

                double learningRate = 0.0001;

                if (dataset.equals(DataSets.DataSet.NP2003) || dataset.equals(DataSets.DataSet.NP2004))
                    learningRate = 0.001;
                else
                    if(dataset.equals(DataSets.DataSet.HP2004))
                        learningRate = 0.00004;

                // With normalization
                System.out.println("DATASET: "+dataset+", LR: "+learningRate+", ITERATIONS: "+iterations+", NORMALISED");
                FoldRunHandler ltrWrapper2 = new FoldRunHandler(handler, dataset, folds, iterations, k, true, learningRate);
                Measurement measurement2 = ltrWrapper2.averageScore();
                String[] values2 = {dataset.name(), ""+iterations, "normalized", ""+measurement2.getEvaluationResult()};
                writer.writeNext(values2);

                // Without normalization
                System.out.println("DATASET: "+dataset+", LR: "+learningRate+", ITERATIONS: "+iterations+", UNNORMALISED");
                FoldRunHandler ltrWrapper = new FoldRunHandler(handler, dataset, folds, iterations, k, false, learningRate);
                Measurement measurement = ltrWrapper.averageScore();
                String[] values = {dataset.name(), ""+iterations, "unnormalized", ""+measurement.getEvaluationResult()};
                writer.writeNext(values);

                try {
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            writer.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}