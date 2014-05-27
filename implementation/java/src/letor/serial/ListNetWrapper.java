package letor.serial;

import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.metric.NDCGScorer;

import java.io.File;
import java.io.FileReader;

/**
 * Created by niek.tax on 5/27/2014.
 */
public class ListNetWrapper {

    public static void main(String[] args) {
        ListNet listNet = new ListNet();
        listNet.printParameters();
        listNet.init();
        File[] train = FileReader("input/ohsumed/Fold1/train.txt");
        listNet.model();
    }
}
