package udf.util;

import ciir.umass.edu.utilities.SimpleMath;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.*;

/**
 * Calculates Ndcg@k
 *
 * Created by niek.tax on 5/30/2014.
 *
 * TODO: Look into using algebraic or accumulator interface for this UDF
 */
public class Ndcg extends EvalFunc<Double>{
    private double[] w;
    private int      k;

    public Ndcg(String paramsString){
        // Read UDF parameters
        String[] params = paramsString.split(";");

        String[] wElemsAsString = params[0].split(",");
        this.w = new double[wElemsAsString.length];
        for(int i = 0; i< wElemsAsString.length; i++) {
            this.w[i] = Double.parseDouble(wElemsAsString[i]);
        }

        this.k = Integer.parseInt(params[1]);
    }

    public Double exec(Tuple input) throws IOException {
        Integer[]       top_k_rels    = new Integer[k];
        PredRelPair[]   top_k_preds   = new PredRelPair[k];

        Arrays.fill(top_k_rels, 0);
        for(int i=0; i<k; i++)
            top_k_preds[i] = new PredRelPair(0.0,0);

        Iterator docBagIterator = ((DataBag) input.get(1)).iterator();
        while(docBagIterator.hasNext()){
            Tuple doc = (Tuple) docBagIterator.next();

            // test if relevence label is larger than one in top_k_rels
            int rel = Integer.parseInt(doc.get(0).toString());
            boolean higher = false;
            int min_i      = 0;
            for(int i=0;i<k;i++){
                if(i>0)
                    min_i = top_k_rels[min_i] > top_k_rels[i] ? i : min_i;
                if(!higher && rel > top_k_rels[i])
                    higher = true;
            }
            if(higher)
                top_k_rels[min_i] = rel;

            int lastAttrIndex = doc.size()-2; // -2 to skip docid
            double[] attrs = new double[lastAttrIndex-2+1]; // -2 to skip rel, qid
            // start loopcounter at 2 to skip rel and qid
            for(int i=2; i<lastAttrIndex;i++){
                // +1 to skip relevance
                attrs[i-2] = Double.parseDouble(doc.get(i+1).toString());
            }
            // Calculate predictions
            double pred = 0.0;
            for(int i=0; i<attrs.length; i++){
                pred += attrs[i]*w[i];
            }

            // test if prediction is larger than one in top_k_preds
            higher = false;
            min_i  = 0;
            for(int i=0;i<k;i++){
                if(i>0)
                    min_i = top_k_preds[min_i].pred > top_k_preds[i].pred ? i : min_i;
                if(!higher && pred > top_k_preds[i].pred)
                    higher = true;
            }
            if(higher) {
                top_k_preds[min_i] = new PredRelPair(pred, rel);
            }
        }
        // Calculate ideal NDCG@k

        // docList is now ordered by relevance
        double idealDCG = getDCG(top_k_rels);

        Integer[] predrels = new Integer[k];
        for(int i=0; i<k; i++)
            predrels[i] = top_k_preds[i].rel;
        double DCG      = getDCG(predrels);
        return (idealDCG>0) ? DCG/idealDCG : 0.0;
    }

    private static double getDCG(Integer[] l){
        Arrays.sort(l, Collections.reverseOrder());
        double dcg = 0.0;

        int i = 1;
        for(int e:l) {
            dcg += (Math.pow(2.0, e) - 1.0) / SimpleMath.logBase2(i + 1);
            i++;
        }
        return dcg;
    }

    private class PredRelPair {
        public final Double pred;
        public final Integer rel;
        public PredRelPair(Double pred, Integer rel) {
            this.pred = pred;
            this.rel = rel;
        }
    }
}
