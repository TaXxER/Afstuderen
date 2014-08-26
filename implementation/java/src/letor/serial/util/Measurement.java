package letor.serial.util;

/**
 * Class that holds the running time and result of an evaluation
 *
 * @author Niek Tax
 */
public class Measurement {
    private static final int timeConversion = 1000000;

    private long    preprocessingTime;
    private long    trainingTime;
    private long    testTime;
    private long    totalTime;
    private double  evaluationResult;

    public Measurement(long start, long afterPre, long afterTrain, long end, double evaluationResult){
        this.preprocessingTime = (afterPre-start)/timeConversion;
        this.trainingTime      = (afterTrain-afterPre)/timeConversion;
        this.testTime          = (end-afterTrain)/timeConversion;
        this.totalTime         = (end-start)/timeConversion;
        this.evaluationResult  = evaluationResult;
    }

    public long getPreprocessingTime(){
        return preprocessingTime;
    }
    public long getTrainingTime(){
        return trainingTime;
    }
    public long getTestTime(){
        return testTime;
    }
    public long getTotalTime(){
        return totalTime;
    }

    public double getEvaluationResult(){
        return evaluationResult;
    }
}
