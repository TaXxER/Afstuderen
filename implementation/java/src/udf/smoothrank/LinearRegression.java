package udf.smoothrank;

import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by niek.tax on 6/6/2014.
 */
public class LinearRegression extends EvalFunc<Tuple> {
    public Tuple exec(Tuple input) throws IOException {
        List<Double> data = new ArrayList<Double>();
        int nobs          = input.size();
        int nvars         =  -1;

        for(int i=0; i<nobs; i++){
            Tuple docTuple = (Tuple) input.get(i);
            if(i==0) // read DIM on first iteration
                nvars = docTuple.size()-2;
            for(int j=0; j<docTuple.size(); j++){
                if(j!=1) // skip qid
                    data.add(Double.parseDouble(docTuple.get(j).toString()));
            }
        }
        System.out.println("input: "+input);
        //OLSMultipleLinearRegression regressor = OLSMultipleLinearRegression();
        //regressor.newSampleData(data.toArray(), nobs, nvars);

        return input;
    }
}
