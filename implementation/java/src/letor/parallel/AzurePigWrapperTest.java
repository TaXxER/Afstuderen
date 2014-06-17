package letor.parallel;

import letor.parallel.util.AzurePigWrapper;

/**
 * Test class for AzurePigWrapper
 *
 * Created by niek.tax on 6/11/2014.
 */
public class AzurePigWrapperTest {
    public static void main(String[] args) throws Exception {
        String pathPrefix   = "/user/hdp";
        String DATASET      = "ohsumed";
        String fold         = "1";
        String outDir       = "oozie_output";
        AzurePigWrapper wrapper = new AzurePigWrapper();

        String pigLine = "TRAIN = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/train.txt' USING PigStorage(' '); " +
                         "TR = FOREACH TRAIN GENERATE $1; "+
                         "STORE TR INTO '"+outDir+"';";

        String result = wrapper.azureRunPig(pigLine, outDir);

        System.out.println("RESULT: "+result);
    }
}
