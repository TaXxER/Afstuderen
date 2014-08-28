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
        int  nodeMemory  = 400000000;
        long mapSplit    = (long)((fileSize / (mappers-1)) * 1.35); // always keep one mapper available for templetonjob. 1.35 roughly adjusts for data size changes in standardisation and feature scaling

        mapSplit = mapSplit > nodeMemory ? nodeMemory : mapSplit;

        String configString =
        "SET job.name 'Learning to Rank';"+
        "SET mapred.task.timeout= 18000000;"+
        "SET mapreduce.task.timeout= 18000000;"+ // For Hadoop 2.3.0 and up
        "SET pig.maxCombinedSplitSize "+mapSplit+";"+
        "SET pig.noSplitCombination true;"+
        "SET mapred.min.split.size "+mapSplit+";"+
        "SET mapreduce.input.fileinputformat.split.maxsize "+mapSplit+";"+ // For Hadoop 2.3.0 and up
        "SET mapred.max.split.size "+mapSplit+";"+
        "SET mapreduce.input.fileinputformat.split.minsize "+mapSplit+";"; // For Hadoop 2.3.0 and up
        //"SET mapred.job.shuffle.input.buffer.percent 0.2;"+
        //"SET mapreduce.reduce.shuffle.input.buffer.percent 0.2;";
        //"SET io.sort.factor 5;"+
        //"SET mapreduce.task.io.sort.factor 5;";
        //"SET io.sort.mb 50;"+
        //"SET mapreduce.task.io.sort.mb 50;";
        return configString;
    }
}
