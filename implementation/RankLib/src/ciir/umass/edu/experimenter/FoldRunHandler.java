package ciir.umass.edu.experimenter;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.learning.*;
import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.SimpleMath;
import ciir.umass.edu.experimenter.util.DataSets;
import ciir.umass.edu.experimenter.parameterizedRankers.AbstractParameterizedRanker;
import ciir.umass.edu.experimenter.parameterizedRankers.ListNetHandler;
import ciir.umass.edu.experimenter.parameterizedRankers.SmoothRankHandler;
import ciir.umass.edu.experimenter.util.Measurement;

import java.util.List;

/**
 * Handles iteration over folds
 * Created by niek.tax on 7/15/2014.
 */
public class FoldRunHandler {
    private MetricScorerFactory mFact = new MetricScorerFactory();

    private String path             = "C:\\Git-data\\Afstuderen\\implementation\\RankLib\\input\\";
    private int folds               = 1;
    private int duplicationNumber   = 0;
    private int k                   = 10;

    private Ranker ranker;

    private String trainMetric = "NDCG@10";
    private String testMetric  = trainMetric;

    private MetricScorer trainScorer = mFact.createScorer(trainMetric);
    private MetricScorer testScorer  = mFact.createScorer(testMetric);

    private Long startTime;
    private Long preTime;
    private Long trainTime;
    private Long endTime;

    private double  learningRate  = 0.00001;
    private boolean normalization = false;

    public FoldRunHandler(AbstractParameterizedRanker ranker, DataSets.DataSet dataSet, int duplicationNumber, int folds, int iterations, int k, boolean normalization, double learningRate){
        this.ranker         = ranker.getParameterizedRanker();
        this.folds          = folds;
        this.k              = k;
        this.normalization  = normalization;
        this.learningRate   = learningRate;

        if(ranker instanceof ListNetHandler){
            ((ListNet)this.ranker).nIteration = iterations;
        }

        if(dataSet.equals(DataSets.DataSet.CUSTOM)) {
            this.duplicationNumber = duplicationNumber;
            this.path        = path + DataSets.getMetaData(DataSets.DataSet.CUSTOM, duplicationNumber).getName() +"\\";
        }else{
            this.path        = path + DataSets.getMetaData(dataSet).getName() +"\\";
        }
    }

    public FoldRunHandler(AbstractParameterizedRanker ranker, DataSets.DataSet dataSet, int folds, int iterations){
        this(ranker, dataSet, 0, folds, iterations, 10, false, 0.00001);
    }

    public FoldRunHandler(AbstractParameterizedRanker ranker, DataSets.DataSet dataSet, int folds, int iterations, int k, boolean normalization, double learningRate){
        this(ranker, dataSet, 0, folds, iterations, k, normalization, learningRate);
    }

    public Measurement averageScore() {
        double scoreSum          = 0.0;

        startTime = System.nanoTime();
        for(int i=0; i<folds; i++){
            //System.out.println("Start Fold "+(i+1)+" evaluation");
            String trainFiles      = path + "Fold"+(i+1)+"\\train.txt";
            String validationFiles = path + "Fold"+(i+1)+"\\vali.txt";
            String testFiles       = path + "Fold"+(i+1)+"\\test.txt";
            double foldScore = evaluate(trainFiles, validationFiles, testFiles);
            //System.out.println("Fold "+(i+1)+" = "+foldScore);
            scoreSum += foldScore;
        }
        endTime = System.nanoTime();

        return new Measurement(startTime, preTime, trainTime, endTime, scoreSum/folds);
    }

    public double evaluate(String trainFile, String validationFile, String testFile){
        List<RankList> train      = readTrainInput(trainFile, duplicationNumber);
        List<RankList> validation = readInput(validationFile);
        List<RankList> test       = readInput(testFile);

        int[] features = getFeatureFromSampleVector(train);

        if(normalization){
            boolean firstdoc = true;
            float[] minmax = new float[2*features.length];
            for(int e=0; e<train.size(); e++){
                for(int d=0; d<train.get(e).size(); d++){
                    for(int f=0; f<features.length; f++){
                        float val = train.get(e).get(d).getFeatureValue(f);
                        if(firstdoc) {
                            minmax[2*f]     = val;
                            minmax[(2*f)+1] = val;
                            firstdoc = false;
                        }else {
                            minmax[2*f]     = val < minmax[2*f] ? val : minmax[2*f];
                            minmax[(2*f)+1] = val > minmax[(2*f)+1] ? val : minmax[(2*f)+1];
                        }
                    }
                }
            }
            train       = normalize(train, minmax);
            validation  = normalize(validation, minmax);
            test        = normalize(test, minmax);
        }

        ranker.set(train, features);
        ranker.set(trainScorer);
        ranker.setValidationSet(validation);
        preTime = System.nanoTime();
        ranker.verbose = false;
        ((ListNet) ranker).learningRate = this.learningRate;
        ranker.init();
        ranker.learn();
        trainTime = System.nanoTime();
        testScorer.setK(k);
        return SimpleMath.round(testScorer.score(ranker.rank(test)), 4);
    }

    public static List<RankList> readInput(String inputFile){
        FeatureManager fm = new FeatureManager();
        List<RankList> samples = fm.read(inputFile, false, false);
        return samples;
    }

    public static List<RankList> readTrainInput(String inputFile, int duplicationNumber){
        if(duplicationNumber==0)
            return readInput(inputFile);
        FeatureManager fm = new FeatureManager();
        List<RankList> samples = null;
        for(int i = 0; i <= duplicationNumber; i++){
            String filePath = new StringBuilder(inputFile).insert(inputFile.length() - 4, String.format("%04d", i)).toString();
            if(i==0) {
                samples = fm.read(filePath, false, false);
            }else
                samples.addAll(fm.read(filePath, false, false));
        }
        return samples;
    }

    public int[] getFeatureFromSampleVector(List<RankList> samples){
        DataPoint dp = samples.get(0).get(0);
        int fc = dp.getFeatureCount();
        int[] features = new int[fc];
        for(int i=0;i<fc;i++)
            features[i] = i+1;
        return features;
    }

    private List<RankList> normalize(List<RankList> rl, float[] minmax){
        for(int i=0; i<rl.size(); i++){
            for(int j=0; j<rl.get(i).size(); j++){
                DataPoint dp = rl.get(i).get(j);
                for(int f=0; f<dp.getFeatureCount(); f++){
                    dp.setFeatureValue(f, dp.getFeatureValue(f) - minmax[2*f] / (minmax[(2*f)+1] - minmax[2*f]));
                }
            }
        }
        return rl;
    }
}
