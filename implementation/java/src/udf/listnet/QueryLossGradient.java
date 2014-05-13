package udf.listnet;

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
 */
public class QueryLossGradient extends EvalFunc<Tuple> {

    private int      ITERATION;

    public QueryLossGradient(String paramsString){
        // Read UDF parameters
        this.ITERATION = Integer.parseInt(paramsString);
    }

    public Tuple exec(Tuple input) throws IOException {
        // Obtain set of data
        DataBag bag;
        if(ITERATION==1)
            bag = (DataBag) (input.get(0));
        else
            bag = (DataBag) (input.get(0));

        // var lossForAQuery = 0.0'
        double   lossForAQuery = 0.0;
        double[] gradientForAQuery = new double[45];

        // Create TupleFactory to generate output Tuple
        TupleFactory tupleFactory = TupleFactory.getInstance();
        Tuple value = tupleFactory.newTuple();

        Iterator<Tuple> dataIterator = bag.iterator();
        while(dataIterator.hasNext()) {
            Tuple dataItem = dataIterator.next();
            double rel = Double.parseDouble(dataItem.get(dataItem.size()-2).toString()); // P_y(j)
            double our = Double.parseDouble(dataItem.get(dataItem.size()-1).toString()); // P_z(j)

            for(int i=0; i<gradientForAQuery.length; i++){
                // gradientForAQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)));
                gradientForAQuery[i] += Double.parseDouble(dataItem.get(i+2).toString()) * (our - rel); // q.docFeatures(j)
            }

            // lossFotAQuery += -P_y(j) * math.log(P_z(j)));
            lossForAQuery += -rel * Math.log(our);
        }

        // Fill output tuple
        value.append(lossForAQuery);
        for(double featureGradient : gradientForAQuery)
            value.append(featureGradient);

        return value;
    }
}
