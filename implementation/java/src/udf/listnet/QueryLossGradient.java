package udf.listnet;

import ciir.umass.edu.utilities.SimpleMath;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 * Used Defined Function (UDF) for Pig Latin
 *
 * Purpose:
 *  1) Calculates loss for the current query
 *  2) Calculates feature gradients based on the current query
 *
 * Created by niek.tax on 5/6/2014.
 *
 * TODO: Look into using algebraic or accumulator interface for this UDF
 */
public class QueryLossGradient extends EvalFunc<Tuple> {
    int DIM;

    public QueryLossGradient(String DIMString){
        this.DIM = Integer.parseInt(DIMString);
    }

    public Tuple exec(Tuple input) throws IOException {
        // Obtain set of data
        DataBag documentBag = (DataBag) (input.get(0));

        // var lossForQuery = 0.0'
        double   lossForQuery = 0.0;
        double[] gradientForQuery = new double[DIM];

        // Create output Tuple
        Tuple value = TupleFactory.getInstance().newTuple();

        double sumRel = 0.0;
        double sumOur = 0.0;
        Iterator<Tuple> docIterator = documentBag.iterator();
        while(docIterator.hasNext()) {
            Tuple docTuple = docIterator.next();
            sumRel += Double.parseDouble(docTuple.get(docTuple.size() - 2).toString()); // P_y(j)
            sumOur += Double.parseDouble(docTuple.get(docTuple.size() - 1).toString()); // P_z(j)
        }

        if(sumRel>0) {
            docIterator = documentBag.iterator();
            while (docIterator.hasNext()) {
                Tuple docTuple = docIterator.next();
                double rel = Double.parseDouble(docTuple.get(docTuple.size() - 2).toString()) / sumRel; // P_y(j)
                double our = sumOur >0 ? Double.parseDouble(docTuple.get(docTuple.size() - 1).toString()) / sumOur : 0; // P_z(j)
                for (int i = 0; i < DIM; i++) {
                    // gradientForQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)));
                    gradientForQuery[i] += Double.parseDouble(docTuple.get(i + 2).toString()) * (our - rel); // q.docFeatures(j). +2 to skip relevance label and qid
                }

                // lossForQuery += -P_y(j) * math.log(P_z(j)));
                if(our == 0)
                    our = 0.00001;
                lossForQuery += -rel * SimpleMath.logBase2(our);
            }
        }

        // Fill output tuple
        value.append(lossForQuery/documentBag.size());
        for(double featureGradient : gradientForQuery)
            value.append(featureGradient);

        return value;
    }
}
