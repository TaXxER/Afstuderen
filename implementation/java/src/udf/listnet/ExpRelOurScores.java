package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.util.List;

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
    private int      ITERATION;

    public ExpRelOurScores(String paramsString){
        // Read UDF parameters
        String[] params = paramsString.split(";");
        String wAsString = params[0];
        this.ITERATION = Integer.parseInt(params[1]);
        String[] wElemsAsString = wAsString.split(",");
        this.w = new double[wElemsAsString.length];

        for(int i = 0; i< wElemsAsString.length; i++) {
            this.w[i] = Double.parseDouble(wElemsAsString[i]);
        }
    }

    public Tuple exec(Tuple input) throws IOException{
        if(input==null || input.size()!=1)
            return null;

        // Obtain set of data
        DataBag bag;
        if(ITERATION==1)
            bag = (DataBag) input.get(0);
        else
            bag = (DataBag) input.get(0);

        Iterator<Tuple> dataIterator = bag.iterator();

        List<Tuple> tupleList = new ArrayList<Tuple>();
        double sumRel = 0.0;
        double sumOur = 0.0;

        while(dataIterator.hasNext()) {
            Tuple dataItem = dataIterator.next();

            // cache for second iteration
            tupleList.add(dataItem);

            // check whether first or non-first iteration
            if(ITERATION==1) {
                // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
                double rel = Double.parseDouble(dataItem.get(0).toString());
                rel = Math.exp(rel);
                dataItem.append(rel);
                sumRel += rel;
            }

            double our = 0.0;
            for(int i=0; i<w.length; i++) {
                // val ourScores = q.docFeatures.map(x => w dot x); (+2 to skip non-feature columns)
                our += Double.parseDouble(dataItem.get(i+2).toString()) * w[i];
            }

            if(ITERATION==1) {
                // val expOurScores = ourScores.map(z => math.exp(z));
                // Append our predicted relevance to query-document pair
                dataItem.append(Math.exp(our));
            }else{
                dataItem.set(dataItem.size()-1, Math.exp(our));
            }
            sumOur += Math.exp(our);
        }

        // val sumExpRelScores = expRelScores.reduce(_ + _);
        // val P_y = expRelScores.map(y => y/sumExpRelScores);
        // val sumExpOurScores = expOurScores.reduce(_ + _);
        // val P_z = expOurScores.map(z => z/sumExpOurScores);
        for(Tuple dataItem : tupleList){
            if(ITERATION==1){
                double rel =  Double.parseDouble(dataItem.get(dataItem.size()-2).toString());
                double normRel = rel / sumRel;
                dataItem.set(dataItem.size()-2, normRel);
            }

            double our =  Double.parseDouble(dataItem.get(dataItem.size()-1).toString());
            double normOur = our / sumOur;
            dataItem.set(dataItem.size()-1, normOur);
        }

        return input;
    }
}