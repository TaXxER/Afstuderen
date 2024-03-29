package udf.smoothrank;

import org.apache.pig.EvalFunc;
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
 *  1) Calculates the elementwise average all columns of a multicolumn relation
 *
 * Created by niek.tax on 5/12/2014.
 */
public class MultiAvg extends EvalFunc<Tuple> {
    public Tuple exec(Tuple input) throws IOException {
        if(input==null || input.size()!=1)
            return null;

        List<Double> avgTupleList = new ArrayList<Double>();

        // Obtain set of data
        DataBag bag = (DataBag) input.get(0);
        long bagSize = bag.size();

        Iterator<Tuple> dataIterator = bag.iterator();
        while(dataIterator.hasNext()) {
            Tuple tuple = dataIterator.next();
            int i = 0;
            for(Object tupElem:tuple.getAll()){
                // Sum
                double current   = avgTupleList.size()>i ? avgTupleList.get(i) : 0.0;
                double increment = Double.parseDouble(tupElem.toString());
                avgTupleList.set(i, current+increment);
                i++;
            }
        }

        TupleFactory tupleFactory = TupleFactory.getInstance();
        Tuple value = tupleFactory.newTuple();
        for(int i=0; i<avgTupleList.size(); i++){
            // Average and write to tuple
            value.append(avgTupleList.get(i) / bagSize);
        }

        return value;
    }
}
