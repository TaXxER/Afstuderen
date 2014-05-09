package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.Iterator;

/**
 * Used Defined Function (UDF) for Pig Latin
 *
 * Purpose:
 *  1) Calculates predicted relevance labels based on w
 *  2) Transforms pred_rel to e^(pred_rel)
 *
 * Created by niek.tax on 5/6/2014.
 */
public class ExpRelOurScores extends EvalFunc<Tuple>{
    private double[] w;

    public ExpRelOurScores(String wAsString){
        String[] wElemsAsString = wAsString.split(",");
        w = new double[wElemsAsString.length];

        for(int i = 0; i< wElemsAsString.length; i++){
            w[i] = Double.parseDouble(wElemsAsString[i]);
        }
    }

    public Tuple exec(Tuple input) throws IOException {
        if(input==null || input.size()!=2)
            return null;

        // Obtain set of data
        DataBag bag = (DataBag) input.get(1);
        Iterator<Tuple> dataIterator = bag.iterator();

        while(dataIterator.hasNext()){
            Tuple dataItem = dataIterator.next();

            // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
            double rel = Double.parseDouble((String) input.get(0));
            rel = Math.exp(rel);
            input.set(0, rel);

            double our = 0.0;
            for(int i=2;i<dataItem.size()-2;i++){
                double featureValue = 0.0;
                // val ourScores = q.docFeatures.map(x => w dot x);
                featureValue = Double.parseDouble((String) input.get(i)) * w[i-2];
                // val expOurScores = ourScores.map(z => math.exp(z));
                our += Math.exp(featureValue);

                // Append our predicted relevance to query-document pair
                dataItem.append(our);
            }
        }

        return input;
    }
}
