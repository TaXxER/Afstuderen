package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

import java.io.IOException;

/**
 * Used Defined Function (UDF) for Pig Latin
 * Transforms relevance labels into e^(rel)
 *
 * Created by niek.tax on 5/6/2014.
 */
public class ExpRelScores extends EvalFunc<Tuple>{
    public Tuple exec(Tuple input) throws IOException {
        if(input==null || input.size()!=1)
            return null;

        // Obtain relevance label
        int     rel = Integer.parseInt((String)input.get(0));
        // Calculate Exp(rel)
        double  ExpRel = Math.exp(rel);
        // Write back to tuple
        input.set(0, ExpRel);

        return input;
    }
}
