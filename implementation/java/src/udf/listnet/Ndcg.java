package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

import java.io.IOException;

/**
 * Calculates Ndcg@k
 *
 * Created by niek.tax on 5/30/2014.
 */
public class Ndcg extends EvalFunc<Tuple> {
    private int k = 10;
    public Ndcg(String paramsString){
        this.k = Integer.parseInt(paramsString);
    }

    public Tuple exec(Tuple input) throws IOException {

    }
}
