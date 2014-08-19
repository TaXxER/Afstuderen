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

    public static String toParamString(List<Double> elemList){
        double[] elemArray = new double[elemList.size()];
        for(int i=0; i<elemArray.length; i++)
            elemArray[i] = elemList.get(i);
        return toParamString(elemArray);
    }

    public static String concatenateStringList(List<String> strings){
        return StringUtils.join(strings, "");
    }

    public static String getPigConfigString(long fileSize, int mappers, int reducers){
        long splitByBytes = (long)((fileSize / (mappers-1)) * 1.35); // always keep one mapper available for templetonjob. 0.7 adjusts for standardisation and feature scaling, which shrink data set by +- 30%
        if (splitByBytes > 6125000){
            splitByBytes = 6125000; // Maximum number of bytes that fits in cluster node memory (512MB physical memory nodes)
        }
        int usedReducers = reducers > 1 ? reducers - 1 : 1; // always keep one reducer available for templetonjob
        String configString =
        "SET default_parallel "+usedReducers+";"+
        "SET job.name 'Learning to Rank';"+
        "SET mapred.task.timeout= 1800000;"+
        "SET mapreduce.task.timeout= 1800000;"+ // For Hadoop 2.3.0 and up
        "SET pig.maxCombinedSplitSize "+splitByBytes+";"+
        "SET pig.noSplitCombination true;"+
        "SET mapred.min.split.size "+splitByBytes+";"+
        "SET mapreduce.input.fileinputformat.split.maxsize "+splitByBytes+";"+ // For Hadoop 2.3.0 and up
        "SET mapred.max.split.size "+splitByBytes+";"+
        "SET mapreduce.input.fileinputformat.split.minsize "+splitByBytes+";"; // For Hadoop 2.3.0 and up
        return configString;
    }
}
