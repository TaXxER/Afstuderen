package letor.serial.parameterizedRankers;

import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import ciir.umass.edu.learning.neuralnet.ListNet;
import letor.serial.FoldRunHandler;

/**
 * Created by niek.tax on 7/15/2014.
 */
public class ListNetHandler implements AbstractParameterizedRanker{
    // PARAMETERS
    private int nIteration = 20000;

    public Ranker getParameterizedRanker() {
        ListNet ranker = new ListNet();
        ranker.nIteration = nIteration;
        return ranker;
    }
}
