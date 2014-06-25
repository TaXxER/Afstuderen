package letor.parallel.util;

import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Utility class that offer utility methods shared between multiple Learning to Rank methods
 *
 * Created by niek.tax on 6/6/2014.
 */
public class LtrUtils {
    // intValue used for both loop-counter and NDCG@k k-value
    public static String toParamString(double[] elems, int intValue){
        String value = "";
        for(double elem : elems)
            value += elem + ",";

        value = value.substring(0,value.length()-1); // remove last comma
        value += ";"+intValue;

        return value;
    }

    public static String toParamString(double[] elems){
        String value = "";
        for(double elem : elems)
            value += elem + ",";

        value = value.substring(0,value.length()-1); // remove last comma

        return value;
    }

    public static String concatenateStringList(List<String> strings){
        return StringUtils.join(strings, "");
    }

    public static String getPigConfigString(long pig_maxCombinedSplitSize, long mapred_min_split_size, long mapred_max_split_size){
        String configString = "SET job.name 'Learning to Rank';"+
        "SET pig.maxCombinedSplitSize "+pig_maxCombinedSplitSize+";"+
        "SET pig.noSplitCombination true;"+
        "SET mapred.min.split.size "+mapred_min_split_size+";"+
        "SET mapreduce.input.fileinputformat.split.maxsize "+mapred_min_split_size+";"+ // For Hadoop 2.3.0 and up
        "SET mapred.max.split.size "+mapred_max_split_size+";"+
        "SET mapreduce.input.fileinputformat.split.minsize "+mapred_max_split_size+";"; // For Hadoop 2.3.0 and up
        return configString;
    }

    public static String getPigConfigString(long fileSize, int mappers, int reducers){
        long splitByBytes = fileSize / (mappers-1); // always keep one mapper available for templetonjob
        int usedReducers = reducers > 1 ? reducers - 1 : 1; // always keep one reducer available for templetonjob
        String configString =
                "SET default_parallel "+usedReducers+";"+
                "SET job.name 'Learning to Rank';"+
                "SET pig.maxCombinedSplitSize "+splitByBytes+";"+
                "SET pig.noSplitCombination true;"+
                "SET mapred.min.split.size "+splitByBytes+";"+
                "SET mapreduce.input.fileinputformat.split.maxsize "+splitByBytes+";"+ // For Hadoop 2.3.0 and up
                "SET mapred.max.split.size "+splitByBytes+";"+
                "SET mapreduce.input.fileinputformat.split.minsize "+splitByBytes+";"; // For Hadoop 2.3.0 and up
        return configString;
    }
}
