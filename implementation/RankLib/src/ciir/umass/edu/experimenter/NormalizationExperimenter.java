package ciir.umass.edu.experimenter;

import ciir.umass.edu.experimenter.util.DataSets;
import ciir.umass.edu.experimenter.parameterizedRankers.AbstractParameterizedRanker;
import ciir.umass.edu.experimenter.parameterizedRankers.ListNetHandler;
import ciir.umass.edu.experimenter.parameterizedRankers.SmoothRankHandler;
import ciir.umass.edu.experimenter.util.Measurement;
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
        datasets.add(DataSets.DataSet.OHSUMED);
        datasets.add(DataSets.DataSet.TD2003);datasets.add(DataSets.DataSet.TD2004);
        datasets.add(DataSets.DataSet.NP2003);datasets.add(DataSets.DataSet.NP2004);
        datasets.add(DataSets.DataSet.HP2003);datasets.add(DataSets.DataSet.HP2004);

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter("normalization_measurements.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        CSVWriter writer = new CSVWriter(out);

        for(DataSets.DataSet dataset: datasets) {
            for (int iterations = 1; iterations < maxIterations; iterations++) {
                // Without normalization
                FoldRunHandler ltrWrapper = new FoldRunHandler(handler, dataset, folds, iterations, k, false);
                Measurement measurement = ltrWrapper.averageScore();
                String[] values = {dataset.name(), ""+iterations, "normalized", ""+measurement.getEvaluationResult()};
                writer.writeNext(values);

                // With normalization
                ltrWrapper = new FoldRunHandler(handler, dataset, folds, iterations, k, true);
                measurement = ltrWrapper.averageScore();
                String[] values2 = {dataset.name(), ""+iterations, "unnormalized", ""+measurement.getEvaluationResult()};
                writer.writeNext(values2);
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