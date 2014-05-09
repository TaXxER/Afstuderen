package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.Iterator;
import java.nio.ByteBuffer;

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
        this.w = new double[wElemsAsString.length];

        for(int i = 0; i< wElemsAsString.length; i++){
            this.w[i] = Double.parseDouble(wElemsAsString[i]);
        }
    }

    public Tuple exec(Tuple input) throws IOException {
        if(input==null || input.size()!=1)
            return null;

        // Obtain set of data
        DataBag bag = (DataBag) input.get(0);

        Iterator<Tuple> dataIterator = bag.iterator();

        while(dataIterator.hasNext()){
            Tuple dataItem = dataIterator.next();

            // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
            System.out.println(dataItem.get(0));
            double rel = Double.parseDouble(dataItem.get(0).toString());
            rel = Math.exp(rel);
            dataItem.set(0, rel);

            double our = 0.0;
            System.out.println("length w: "+w.length);
            System.out.println("dataItem size: "+dataItem.size());
            for(int i=0; i<w.length; i++){
                System.out.println("w["+(i)+"]: "+w[i]);
                System.out.println("feature "+(i)+": "+Double.parseDouble(dataItem.get(i+2).toString()));

                // val ourScores = q.docFeatures.map(x => w dot x);
                double featureValue = Double.parseDouble(dataItem.get(i+2).toString()) * w[i];

                // val expOurScores = ourScores.map(z => math.exp(z));
                our += Math.exp(featureValue);

                // Append our predicted relevance to query-document pair
                dataItem.append(our);
            }
        }

        return input;
    }
}
