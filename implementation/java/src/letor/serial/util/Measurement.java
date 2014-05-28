package letor.serial.util;

/**
 * Class that holds the running time and result of an evaluation
 *
 * @author Niek Tax
 */
public class Measurement {
    private long    runningTime;
    private double  evaluationResult;

    public Measurement(long runningTime, double evaluationResult){
        this.runningTime      = runningTime;
        this.evaluationResult = evaluationResult;
    }

    public long getRunningTime(){
        return runningTime;
    }

    public double getEvaluationResult(){
        return evaluationResult;
    }
}
