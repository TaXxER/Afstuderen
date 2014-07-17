package letor.serial;

import ciir.umass.edu.learning.{RankList, Ranker}
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer
import org.codehaus.jet.regression.estimators.OLSMultipleLinearRegressionEstimator

/*
 * Implements SmoothRank
 * @Author Niek Tax
 */
class SmoothRank extends Ranker{
  //Parameters
  val smoothFactor             = 0.01
  val initIterations           = 10
  val initEps                  = 0.01 // step size

  //Local variables
  var w:Array[Double] = null

  override def init() {
    PRINT("Initializing... ")
    val samplesArray = samples.toArray
    //PRINTLN("BEFORE olsInput")
    //PRINTLN("samplesArray.length: "+samplesArray.length)
    //val olsInput = samplesArray.flatMap(x => toOlsInput(x.asInstanceOf[RankList]))
    //PRINTLN("AFTER olsInput")
    //val ols = new OLSMultipleLinearRegression()
    val ols2 = new OLSMultipleLinearRegressionEstimator()
    samplesArray.foreach(x => addToOLS(x.asInstanceOf[RankList], ols2))

    w = ols2.estimateRegressionParameters()
    PRINTLN("w: "+w)

    //ols.newSampleData(olsInput, olsInput.length/features.length, features.length-1)

    //w = ols.estimateRegressionParameters()


    //var cost = 0.0
    //cost = Math.pow(2, olsInput.foreach(cost += _) )
    //val q = samples.map(x:RankList => ((RankList) x).)

    PRINTLN("[DONE]")
  }

  override def learn() {
    PRINTLN("Called Learn")
    // Calculate starting point by regression on gain function values
    val optimizer = new NonLinearConjugateGradientOptimizer(NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE, null)
  }

  def toOlsInput(rl: RankList):Array[Double] = {
    var olsList = List[Double]()
    for(i <- 1 until rl.size()){
      val dp = rl.get(i)
      olsList = olsList :+ (Math.pow(2, dp.getLabel) - 1)
      for(j <- 1 until features.length)
        olsList = olsList :+ dp.getFeatureValue(j).asInstanceOf[Double]
    }
    PRINTLN("KLAAR!")
    return olsList.toArray
  }

  def addToOLS(rl: RankList, ols:OLSMultipleLinearRegressionEstimator){
    var x = List[Array[Double]]()
    var y = List[Double]()
    for(i <- 1 until rl.size()){
      var xElem = List[Double]()
      val dp = rl.get(i)
      for(j <- 1 until features.length)
        xElem = xElem :+ (dp.getFeatureValue(j).asInstanceOf[Double] + (Math.random()*Double.MinValue)) // add small random value to guarantee matrix to be non-singular
      y = y :+ (Math.pow(2, dp.getLabel) -1)
      x = x :+ xElem.toArray
    }

    if(x.size>0 && y.size>0)
      ols.addData(y.toArray, x.toArray, null)
  }
}