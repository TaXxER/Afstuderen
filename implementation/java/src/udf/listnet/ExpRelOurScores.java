package udf.listnet;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Used Defined Function (UDF) for Pig Latin
 *
 * Purpose:
 *  1) Calculates predicted relevance labels based on w
 *  2) Transforms pred_rel to e^(pred_rel)
 *
 * Created by niek.tax on 5/6/2014.
 *
 * TODO: Look into using algebraic or accumulator interface for this UDF
 */
public class ExpRelOurScores extends EvalFunc<Tuple>{
    private double[] w;
    private int      ITERATION;

    public ExpRelOurScores(String paramsString){
        // Read UDF parameters
        String[] params = paramsString.split(";");
        String wAsString = params[0];
        this.ITERATION = Integer.parseInt(params[1]);
        String[] wElemsAsString = wAsString.split(",");
        this.w = new double[wElemsAsString.length];

        for(int i = 0; i< wElemsAsString.length; i++) {
            this.w[i] = Double.parseDouble(wElemsAsString[i]);
        }
    }

    public Tuple exec(Tuple input) throws IOException{
        if(input==null || input.size()!=1)
            return null;
        // Obtain set of data
        Iterator<Tuple> docIterator = ((DataBag) input.get(0)).iterator();
        double sumRel = 0.0;
        BigDecimal sumOur = BigDecimal.ZERO;
        while(docIterator.hasNext()) {
            Tuple docTuple = docIterator.next();

            // check whether first or non-first ListNet iteration
            if(ITERATION==1) {
                // val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
                double rel = Double.parseDouble(docTuple.get(0).toString());
                rel = Math.exp(rel);
                docTuple.append(rel);
                sumRel += rel;
            }
            double our = 0.0;
            for(int i=0; i<w.length; i++) {
                // val ourScores = q.docFeatures.map(x => w dot x); (+2 to skip non-feature columns)
                double augend = Double.parseDouble(docTuple.get(i+2).toString()) * w[i];
                our += augend;
            }
            BigDecimal bdOur = BigDecimal.valueOf(our);
            BigDecimal bdExpOur = expTaylor(bdOur, 4);
            // val expOurScores = ourScores.map(z => math.exp(z));
            if(ITERATION==1)
                docTuple.append(bdExpOur.toString());
            else
                docTuple.set(docTuple.size() - 1, bdExpOur.toString());
            sumOur = sumOur.add(bdExpOur);
        }
        // val sumExpRelScores = expRelScores.reduce(_ + _);
        // val P_y = expRelScores.map(y => y/sumExpRelScores);
        // val sumExpOurScores = expOurScores.reduce(_ + _);
        // val P_z = expOurScores.map(z => z/sumExpOurScores);
        Iterator<Tuple> docIterator2 = ((DataBag) input.get(0)).iterator();
        while(docIterator2.hasNext()) {
            Tuple dataItem = docIterator2.next();
            // Exp(relevance) only calculated in first iteration, as it is constant
            if (ITERATION == 1) {
                double rel = Double.parseDouble(dataItem.get(dataItem.size() - 2).toString());
                dataItem.set(dataItem.size() - 2, rel / sumRel);
            }
            double our = Double.parseDouble(dataItem.get(dataItem.size() - 1).toString());
            dataItem.set(dataItem.size() - 1, BigDecimal.valueOf(our).divide(sumOur, sumOur.precision(), RoundingMode.HALF_UP).doubleValue());
        }
        return input;
    }

    /**
     * Compute e^x to a given scale by the Taylor series.
     * @param x the value of x
     * @param scale the desired scale of the result
     * @return the result value
     */
    private static BigDecimal expTaylor(BigDecimal x, int scale) {
        BigDecimal factorial = BigDecimal.valueOf(1);
        BigDecimal xPower    = x;
        BigDecimal sumPrev;

        // 1 + x
        BigDecimal sum  = x.add(BigDecimal.valueOf(1));

        // Loop until the sums converge
        // (two successive sums are equal after rounding).
        int i = 2;
        do {
            // x^i
            xPower = xPower.multiply(x)
                    .setScale(scale, BigDecimal.ROUND_HALF_EVEN);

            // i!
            factorial = factorial.multiply(BigDecimal.valueOf(i));

            // x^i/i!
            BigDecimal term = xPower
                    .divide(factorial, scale,
                            BigDecimal.ROUND_HALF_EVEN);

            // sum = sum + x^i/i!
            sumPrev = sum;
            sum = sum.add(term);

            ++i;
        } while (sum.compareTo(sumPrev) != 0);

        return sum;
    }

}