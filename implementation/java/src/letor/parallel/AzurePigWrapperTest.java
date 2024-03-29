package letor.parallel;

import letor.parallel.util.HDInsightWrapper;

/**
 * Test class for HDInsightWrapper
 *
 * Created by niek.tax on 6/11/2014.
 */
public class AzurePigWrapperTest {
    public static void main(String[] args) throws Exception {
        String pathPrefix   = "/user/hdp";
        String DATASET      = "ohsumed";
        String fold         = "1";
        String outDir       = "oozie_output";

        // cluster configuration
        String clusterName          = "ltrmini";
        String clusterUser          = "admin";
        String clusterPassword      = "Qw!23456789";
        String storageAccount       = "ltrstorage";
        String storageAccountKey    = "igtZD3Jih9lsvxoIcxCury1GDqS7Z4DQ0Ci7xVphY9p/6rnwaHG5qZFBKXjt0wOgwNwVqno5sitAy/eucuPGMA==";

        HDInsightWrapper wrapper = new HDInsightWrapper(clusterName, clusterUser, clusterPassword, storageAccount, storageAccountKey, 1);

        String pigLine = "TRAIN = LOAD '" + pathPrefix + "/input/" + DATASET + "/Fold" + fold + "/train.txt' USING PigStorage(' '); " +
                         "TR = FOREACH TRAIN GENERATE $1; "+
                         "STORE TR INTO '"+outDir+"';";

        String result = wrapper.runPig(pigLine, outDir);

        System.out.println("RESULT: "+result);
    }
}
