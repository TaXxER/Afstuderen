package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.Iterator;

/**
 * Used Defined Function (UDF) for Pig Latin
 * Transforms relevance labels into e^(rel)
 *
 * Created by niek.tax on 5/6/2014.
 */
public class ExpOurScores extends EvalFunc<Tuple>{
    private double[] w;

    public ExpOurScores(double[] w){
        this.w = w;
    }

    public Tuple exec(Tuple input) throws IOException {
        if(input==null || input.size()!=2)
            return null;

        // Obtain set of data
        DataBag bag = (DataBag) input.get(1);
        Iterator<Tuple> dataIterator = bag.iterator();

        while(dataIterator.hasNext()){
            Tuple dataItem = dataIterator.next();

            for(int i=2;i<dataItem.size()-2;i++){
                double featureValue = Double.parseDouble((String) input.get(i));
                // val ourScores = q.docFeatures.map(x => w dot x);
                featureValue *= w[i-2];
                // val expOurScores = ourScores.map(z => math.exp(z));
                featureValue = Math.exp(featureValue);

                input.set(i, featureValue);
            }
        }

        return input;
    }
}
