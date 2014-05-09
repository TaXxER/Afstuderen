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
    public Tuple exec(Tuple input) throws IOException {
        // Obtain set of data
        DataBag bag = (DataBag) input.get(0);

        double   lossForAQuery = 0.0;
        double[] gradientForAQuery = new double[45];

        // Create TupleFactory to generate output Tuple
        TupleFactory tupleFactory = TupleFactory.getInstance();
        Tuple value = tupleFactory.newTuple();

        Iterator<Tuple> dataIterator = bag.iterator();
        while(dataIterator.hasNext()) {
            Tuple dataItem = dataIterator.next();
            double rel = Double.parseDouble(dataItem.get(dataItem.size()-2).toString());
            double our = Double.parseDouble(dataItem.get(dataItem.size()-1).toString());
            lossForAQuery += -rel * Math.log(our);
            // TODO: Calculate Query Loss and Feature Gradients
        }

        // Fill output tuple
        value.append(lossForAQuery);
        for(double featureGradient : gradientForAQuery)
            value.append(featureGradient);

        return value;
    }
}
