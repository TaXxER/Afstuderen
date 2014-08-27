package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Used Defined Function (UDF) for Pig Latin
 *
 * Purpose:
 *  1) Calculates the elementwise sum of all columns of a multicolumn relation
 *
 * Created by niek.tax on 5/12/2014.
 *
 * TODO: Look into using algebraic or accumulator interface for this UDF
 */
public class MultiSum extends EvalFunc<Tuple> {
    public Tuple exec(Tuple input) throws IOException {
        if(input==null || input.size()!=2)
            return null;

        List<Double> sumTupleList = new ArrayList<Double>();

        // Obtain set of data
        DataBag bag = (DataBag) input.get(1);
        Iterator<Tuple> dataIterator = bag.iterator();
        while(dataIterator.hasNext()) {
            Tuple tuple = dataIterator.next();
            for(int i=0; i < tuple.size(); i++){
                // Sum
                double increment = Double.parseDouble(tuple.get(i).toString());
                if(sumTupleList.size()>i) {
                    sumTupleList.set(i, sumTupleList.get(i) + increment);
                }else {
                    sumTupleList.add(increment);
                }
            }
        }

        return TupleFactory.getInstance().newTuple(sumTupleList);
    }
}
