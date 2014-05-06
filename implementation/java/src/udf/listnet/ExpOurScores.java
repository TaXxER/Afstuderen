package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

import java.io.IOException;

/**
 * Created by niek.tax on 5/6/2014.
 */
public class ExpOurScores extends EvalFunc<Tuple> {
    private double[] w;

    public ExpOurScores(double[] w){
        this.w = w;
    }

    public Tuple exec(Tuple input) throws IOException {
        if(input==null || input.size()!=1)
            return null;
        // Only process feature columns: skip first two indices and last index
        for(int i=2;i<input.size()-2;i++){
            // Obtain relevance label
            double  rel = Double.parseDouble((String) input.get(i));
            // Calculate Exp(rel)
            double  ExpRel = Math.exp(rel*w[i-2]);
            // Write back to tuple
            input.set(0, ExpRel);
        }

        return input;
    }

}
