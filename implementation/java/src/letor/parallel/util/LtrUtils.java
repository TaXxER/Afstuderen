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
}
