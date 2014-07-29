package letor.serial;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.learning.*;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.SimpleMath;
import letor.serial.parameterizedRankers.AbstractParameterizedRanker;
import letor.serial.util.Measurement;

import java.util.List;

/**
 * Handles iteration over folds
 * Created by niek.tax on 7/15/2014.
 */
public class FoldRunHandler {
    private MetricScorerFactory mFact = new MetricScorerFactory();

    private String path  = "C:\\Git-data\\Afstuderen\\implementation\\java\\input\\";
    private int folds    = 1;

    private Ranker ranker;

    private String trainMetric = "NDCG@10";
    private String testMetric  = trainMetric;

    private MetricScorer trainScorer = mFact.createScorer(trainMetric);
    private MetricScorer testScorer  = mFact.createScorer(testMetric);

    public FoldRunHandler(AbstractParameterizedRanker ranker, String pathPostFix){
        this.ranker      = ranker.getParameterizedRanker();
        path = path + pathPostFix +"\\";
    }

    public Measurement averageScore() {
        double scoreSum          = 0.0;

        Long startTime = System.nanoTime();
        for(int i=0; i<folds; i++){
            System.out.println("Start Fold "+(i+1)+" evaluation");
            String trainFiles      = path + "Fold"+(i+1)+"\\train.txt";
            String validationFiles = path + "Fold"+(i+1)+"\\vali.txt";
            String testFiles       = path + "Fold"+(i+1)+"\\test.txt";
            double foldScore = evaluate(trainFiles, validationFiles, testFiles);
            System.out.println("Fold "+(i+1)+" = "+foldScore);
            scoreSum += foldScore;
        }
        Long endTime = System.nanoTime();

        return new Measurement((endTime-startTime)/1000000, scoreSum/folds);
    }

    public double evaluate(String trainFile, String validationFile, String testFile){
        List<RankList> train      = readInput(trainFile);
        List<RankList> validation = readInput(validationFile);
        List<RankList> test       = readInput(testFile);

        int[] features = getFeatureFromSampleVector(train);
        ranker.set(train, features);
        Ranker.verbose = true;
        ranker.set(trainScorer);
        ranker.setValidationSet(validation);
        ranker.init();
        ranker.learn();

        return SimpleMath.round(testScorer.score(ranker.rank(test)), 4);
    }

    public static List<RankList> readInput(String inputFile){
        FeatureManager fm = new FeatureManager();
        List<RankList> samples = fm.readInput(inputFile, false, false);
        return samples;
    }

    public int[] getFeatureFromSampleVector(List<RankList> samples)
    {
        DataPoint dp = samples.get(0).get(0);
        int fc = dp.getFeatureCount();
        int[] features = new int[fc];
        for(int i=0;i<fc;i++)
            features[i] = i+1;
        return features;
    }
}
