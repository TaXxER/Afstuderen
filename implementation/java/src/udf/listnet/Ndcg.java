package udf.listnet;

import ciir.umass.edu.utilities.SimpleMath;
import com.google.common.collect.TreeMultiset;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import udf.listnet.util.Document;

import java.io.IOException;
import java.util.*;

/**
 * Calculates Ndcg@k
 *
 * Created by niek.tax on 5/30/2014.
 */
public class Ndcg extends EvalFunc<Double> {
    private double[] w;
    private int      k;

    public Ndcg(String paramsString){
        // Read UDF parameters
        String[] params = paramsString.split(";");
        String wAsString = params[0];
        this.k = Integer.parseInt(params[1]);
        String[] wElemsAsString = wAsString.split(",");
        this.w = new double[wElemsAsString.length];

        for(int i = 0; i< wElemsAsString.length; i++) {
            this.w[i] = Double.parseDouble(wElemsAsString[i]);
        }
    }

    public Double exec(Tuple input) throws IOException {
        // Obtain MultiSet of documents
        TreeMultiset<Document> docBag = TreeMultiset.create();
        DataBag docDataBag = (DataBag) input.get(1);
        Iterator docBagIterator = docDataBag.iterator();
        int j = 1;
        while(docBagIterator.hasNext()){
            Tuple doc = (Tuple) docBagIterator.next();
            // -2 to skip docid
            int lastAttrIndex = doc.size()-2;
            int rel = Integer.parseInt(doc.get(0).toString());
            // -2 to skip rel and qid
            double[] attrs = new double[lastAttrIndex-2+1];
            // start loopcounter at 2 to skip rel and qid
            for(int i=2; i<lastAttrIndex;i++){
                // +1 to skip relevance
                attrs[i-2] = Double.parseDouble(doc.get(i+1).toString());
            }
            docBag.add(new Document(attrs, w, rel));
            j++;
        }

        // Calculate ideal NDCG@k
        LinkedList docList = new LinkedList<Document>();
        // docList is now ordered by relevance
        docList.addAll(docBag);
        double idealDCG = getDCG(docList, k);
        // Sort on predicted relevance for DCG@k calculation
        Collections.reverse(docList);
        Collections.sort(docList, Document.getPredictedComparator());
        double DCG = getDCG(docList, k);
        return (idealDCG>0) ? DCG/idealDCG : 0.0;
    }

    private static double getDCG(LinkedList<Document> rel, int k){
        int size = k;
        if(k > rel.size() || k <= 0)
            size = rel.size();
        double dcg = 0.0;

        Iterator<Document> iter = rel.iterator();
        int i = 1;
        while(iter.hasNext() && i<=size){
            dcg += (Math.pow(2.0, iter.next().getRelevance())-1.0)/SimpleMath.logBase2(i+1);
            i++;
        }
        return dcg;
    }
}
