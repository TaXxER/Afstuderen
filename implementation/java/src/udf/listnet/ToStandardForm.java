package udf.listnet;

import org.apache.commons.lang.StringUtils;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;

/**
 * Transforms loaded data file into a standard form of rel, attributes.
 * Strips data from docid and other unused columns if present.
 *
 * Created by niek.tax on 6/3/2014.
 */
public class ToStandardForm extends EvalFunc<Tuple> {
    public Tuple exec(Tuple input) throws IOException {
        // Create returnTuple
        Tuple returnTuple = TupleFactory.getInstance().newTuple();
        // Set relevance
        returnTuple.append(input.get(0));
        // Determine and set query id
        returnTuple.append(input.get(1).toString().split(":")[1]);

        // Skip relevance, start at index 2
        for(int i=2; i<input.size(); i++){
            Object obj = input.get(i);
            if(obj==null)
                continue;
            String[] parts = obj.toString().split(":");
            // Identify attributes, append to returnTuple
            if(parts.length==2 && StringUtils.isNumeric(parts[0])) {
                returnTuple.append(parts[1]);
            }
        }
        return returnTuple;
    }
}
