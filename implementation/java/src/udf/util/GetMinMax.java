package udf.util;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Extracts the maximum and the minimum value per feature
 *
 * Created by niek.tax on 7/1/2014.
 */
public class GetMinMax extends EvalFunc<Tuple> {

    public Tuple exec(Tuple input) throws IOException {
        ArrayList<Double> minmaxList = new ArrayList<Double>(); // holds minimum feature values on uneven and maximum feature values on even positions

        // Initialize minList and maxList with feature values of first document
        DataBag docBag = (DataBag) input.get(1);
        Iterator<Tuple> docTupleIterator = docBag.iterator();
        Tuple firstDoc = docTupleIterator.next();
        for(int i=2; i<firstDoc.size(); i++) {
            double ithElem = Double.parseDouble(firstDoc.get(i).toString());
            minmaxList.add(ithElem);
            minmaxList.add(ithElem);
        }

        // Find minimum and maximum values per feature
        while(docTupleIterator.hasNext()){
            Tuple doc = docTupleIterator.next();
            for(int i=2; i<firstDoc.size(); i++) {
                double ithElem = Double.parseDouble(doc.get(i).toString());

                int minIndex = 2*(i-1)-2; int maxIndex = 2*(i-1)-1;

                if(minmaxList.get(minIndex) > ithElem)
                    minmaxList.set(minIndex, ithElem);
                if(minmaxList.get(maxIndex) < ithElem)
                    minmaxList.set(maxIndex, ithElem);
            }
        }

        Tuple resultTuple = TupleFactory.getInstance().newTuple();

        for(Double minmax : minmaxList)
            resultTuple.append(minmax);

        return resultTuple;
    }
}
