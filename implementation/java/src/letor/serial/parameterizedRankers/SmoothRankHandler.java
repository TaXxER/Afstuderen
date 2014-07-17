package letor.serial.parameterizedRankers;

import ciir.umass.edu.learning.Ranker;
import letor.serial.SmoothRank;

/**
 * Created by niek.tax on 7/15/2014.
 */
public class SmoothRankHandler implements AbstractParameterizedRanker{
    // PARAMETERS

    public Ranker getParameterizedRanker() {
        SmoothRank ranker = new SmoothRank();
        return ranker;
    }
}
