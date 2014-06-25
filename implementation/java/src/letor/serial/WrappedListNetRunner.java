package letor.serial;

import letor.serial.util.Measurement;

/**
 * Created by niek.tax on 5/28/2014.
 */
public class WrappedListNetRunner {
    public static void main(String[] args){
        ListNetWrapper listNetWrapper = new ListNetWrapper();
        Measurement measurement = listNetWrapper.averageScore("ohsumed");
        System.out.println("Time elapsed: "+measurement.getRunningTime());
        System.out.println("NDCG@10:      "+measurement.getEvaluationResult()+" ms");
    }
}
