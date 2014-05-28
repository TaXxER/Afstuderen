package letor.serial;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.learning.*;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.SimpleMath;
import letor.serial.util.Measurement;

import java.util.List;

/**
 * RankLib evaluation wrapper.
 * Calculates average running time and evaluation results on a prefolded dataset.
 *
 * @author Niek Tax
 */
public class ListNetWrapper {
    // java -jar bin/RankLib.jar -train C:\Git-data\Afstuderen\implementation\java\input\ohsumed\Fold1\train.txt -ranker 7 -metric2t NDCG@10 -gmax 2 -validate C:\Git-data\Afstuderen\implementation\java\input\ohsumed\Fold1\train.txt -test C:\Git-data\Afstuderen\implementation\java\input\ohsumed\Fold1\test.txt
    private MetricScorerFactory mFact = new MetricScorerFactory();

    private String path  = "C:\\Git-data\\Afstuderen\\implementation\\java\\input\\MQ2007\\";
    private int folds    = 5;

    private RANKER_TYPE rType  = RANKER_TYPE.LISTNET;
    private String trainMetric = "NDCG@10";
    private String testMetric  = trainMetric;

    private MetricScorer trainScorer = mFact.createScorer(trainMetric);
    private MetricScorer testScorer  = mFact.createScorer(testMetric);

    public Measurement averageScore() {
        double scoreSum          = 0.0;

        Long startTime = System.nanoTime();
        for(int i=0; i<folds; i++){
            System.out.println("Start Fold "+(i+1)+"evaluation");
            String trainFiles      = path + "Fold"+(i+1)+"\\train.txt";
            String validationFiles = path + "Fold"+(i+1)+"\\vali.txt";
            String testFiles       = path + "Fold"+(i+1)+"\\test.txt";
            double foldScore = evaluate(trainFiles, validationFiles, testFiles);
            System.out.println("Fold "+(i+1)+" = "+foldScore);
            scoreSum += foldScore;
        }
        Long endTime = System.nanoTime();

        return new Measurement(endTime-startTime, scoreSum/folds);
    }

    public double evaluate(String trainFile, String validationFile, String testFile){
        List<RankList> train      = readInput(trainFile);
        List<RankList> validation = readInput(validationFile);
        List<RankList> test       = readInput(testFile);

        int[] features = getFeatureFromSampleVector(train);
        RankerFactory rFact = new RankerFactory();
        Ranker ranker = rFact.createRanker(rType, train, features);
        Ranker.verbose = false;
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