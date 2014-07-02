package udf.util;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Combines GetMinMax()-output into a single minmax vector
 *
 * Created by niek.tax on 7/1/2014.
 */
public class CombineMinMax extends EvalFunc<Tuple> {

    public Tuple exec(Tuple input) throws IOException {
        DataBag minmaxBag = (DataBag) input.get(1);
        Iterator<Tuple> minmaxIter = minmaxBag.iterator();
        List<Double> minmaxList = new ArrayList<Double>();
        Tuple firstMinmax = minmaxIter.next();
        for(int i=0; i<firstMinmax.size(); i++){
            minmaxList.add((Double) firstMinmax.get(i));
        }
        while(minmaxIter.hasNext()){
            Tuple minmax = minmaxIter.next();
            for(int i=0; i<minmax.size(); i++){
                double ithElem = (Double) minmax.get(i);
                if(i%2==0){ // MIN
                    if (ithElem < minmaxList.get(i))
                        minmaxList.set(i, ithElem);
                }else{ // MAX
                    if (ithElem > minmaxList.get(i))
                        minmaxList.set(i, ithElem);
                }
            }
        }

        Tuple resultTuple = TupleFactory.getInstance().newTuple();

        for(Double minmax: minmaxList)
            resultTuple.append(minmax);

        return resultTuple;
    }
}