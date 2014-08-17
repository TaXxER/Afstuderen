package udf.util;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Scales features using defined MINIMUM and MAXIMUM feature values
 *
 * Created by niek.tax on 7/2/2014.
 */
public class ScaleFeatures extends EvalFunc<Tuple>{
    LinkedList<Double> minValues;
    LinkedList<Double> maxValues;

    public ScaleFeatures(String paramString){
        minValues = new LinkedList<Double>();
        maxValues = new LinkedList<Double>();
        String[] paramStringArray = paramString.split(",");
        for(int i=0; i<paramStringArray.length; i++){
            Double paramValue = Double.parseDouble(paramStringArray[i]);
            if(i%2==0)
                minValues.add(paramValue);
            else
                maxValues.add(paramValue);
        }
    }

    public Tuple exec(Tuple input) throws IOException{
        // skip two: relevance label and qid
        for(int i=2; i<input.size(); i++){
            // (x - min(x)) / (max(x)-min(x))
            if(maxValues.get(i-2)-minValues.get(i-2) > 0) { // prevents possible dividing by 0 scenario
                double unscaled = Double.parseDouble(input.get(i).toString());
                double scaled = (unscaled - minValues.get(i - 2)) / (maxValues.get(i - 2) - minValues.get(i - 2));
                input.set(i, scaled);
            }
        }
        return input;
    }
}
