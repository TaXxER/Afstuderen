package udf.smoothrank;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by niek.tax on 6/6/2014.
 */
public class ExpRelOurScores extends EvalFunc<Tuple> {
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

        // On first iteration: append Gain-function value to rows
        // Step 1: Rank documents based on w
        // Step 2: Calculate h_{i,j} based on true rankings
        // Step 3: Calculate Aq based on current w
        // Step 4: Calculate gradients based on

        // Obtain set of data
        DataBag docBag = (DataBag) input.get(0);
        Iterator<Tuple> docIterator = docBag.iterator();
        List<Tuple> docTupleList = new ArrayList<Tuple>();
        double sumRel = 0.0;
        double sumOur = 0.0;

        while(docIterator.hasNext()) {
            Tuple docTuple = docIterator.next();

            // cache for second iteration
            docTupleList.add(docTuple);

            // check whether first or non-first ListNet iteration
            if(ITERATION==1) {
                // set Gain to 2^rel-1
                double rel = Double.parseDouble(docTuple.get(0).toString());
                rel = Math.exp(rel);
                docTuple.append(rel);
                sumRel += rel;
            }

            double our = 0.0;
            for(int i=0; i<w.length; i++) {
                // val ourScores = q.docFeatures.map(x => w dot x); (+2 to skip non-feature columns)
                our += Double.parseDouble(docTuple.get(i+2).toString()) * w[i];
            }

            if(ITERATION==1) {
                // val expOurScores = ourScores.map(z => math.exp(z));
                // Append our predicted relevance to query-document pair
                docTuple.append(Math.exp(our));
            }else{
                docTuple.set(docTuple.size()-1, Math.exp(our));
            }
            sumOur += Math.exp(our);
        }

        // val sumExpRelScores = expRelScores.reduce(_ + _);
        // val P_y = expRelScores.map(y => y/sumExpRelScores);
        // val sumExpOurScores = expOurScores.reduce(_ + _);
        // val P_z = expOurScores.map(z => z/sumExpOurScores);
        for(Tuple dataItem : docTupleList){
            // Exp(relevance) only calculated in first iteration, as it is constant
            if(ITERATION==1){
                double rel =  Double.parseDouble(dataItem.get(dataItem.size()-2).toString());
                dataItem.set(dataItem.size()-2, rel / sumRel);
            }

            double our =  Double.parseDouble(dataItem.get(dataItem.size()-1).toString());
            dataItem.set(dataItem.size()-1, our / sumOur);
        }

        return input;
    }
}
