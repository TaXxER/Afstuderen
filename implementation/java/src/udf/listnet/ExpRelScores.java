package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 * Pig UDF that does two things:
 *  1) Transforms relevance label to exp(rel)
 *  2) Strips feature-data from DataBag
 */
public class ExpRelScores extends EvalFunc<Tuple> {
    public Tuple exec(Tuple input) throws IOException {
        if(input==null || input.size()!=2)
            return null;

        TupleFactory tupleFactory = TupleFactory.getInstance();
        BagFactory   bagFactory   = BagFactory.getInstance();

        // Create new Bag with smaller tuples
        DataBag replacementBag = bagFactory.newDefaultBag();

        // Obtain set of data
        int qid     = Integer.parseInt((String) input.get(0));
        DataBag bag = (DataBag) input.get(1);

        Iterator<Tuple> dataIterator = bag.iterator();
        while(dataIterator.hasNext()){
            Tuple dataItem = dataIterator.next();
            // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
            double rel = Double.parseDouble((String) input.get(0));
            rel = Math.exp(rel);

            // Read document ID
            int doc_id     = Integer.parseInt((String) input.get(input.size()-1));

            // Create new Tuple with only qid, doc_id and exp(rel)
            Tuple replacementTuple = tupleFactory.newTuple(3);
            replacementTuple.set(0, rel);
            replacementTuple.set(1, qid);
            replacementTuple.set(2, doc_id);
            replacementBag.add(replacementTuple);
        }

        input.set(1, replacementBag);
        return input;
    }
}
