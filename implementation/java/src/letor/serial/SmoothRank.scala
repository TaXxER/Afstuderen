package letor.serial;

import ciir.umass.edu.learning.{RankList, Ranker, DataPoint}
import ciir.umass.edu.utilities.Sorter
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer
import org.ejml.alg.dense.linsol.svd.SolvePseudoInverseSvd
import org.ejml.data.{DenseMatrix64F, ReshapeMatrix64F}
import org.ejml.ops.CommonOps
import scala.collection.JavaConverters._

/*
 * Implements SmoothRank
 * @Author Niek Tax
 */
class SmoothRank extends Ranker{
  //Parameters
  val initialSF                = Math.pow(2,  6).toFloat
  val stoppingSF               = Math.pow(2, -6).toFloat
  val initIterations           = 10
  val initEps                  = 0.01 // step size
  val k                        = 10

  //Local variables
  var w:Array[Float] = null
  var scaledSamples:Array[RankList] = null
  var lambda                   = 1

  override def init() {
    PRINT("Initializing... ")

    scaledSamples = scaleFeatures(samples, features.length)
    val spi = new SolvePseudoInverseSvd()

    val numDps = scaledSamples.map(x => x.size).reduceLeft(_+_)
    var A = new DenseMatrix64F(numDps, features.length)
    var B = new DenseMatrix64F(numDps, 1)
    var X = new DenseMatrix64F(features.length, 1)
    scaledSamples.foreach(x => addToPseudoSolver(x, A, B))
    spi.setA(A)
    spi.solve(B,X)

    val w0 = X.getData.map(x => x.toFloat)
    w = w0

    PRINTLN("[DONE]")
  }

  override def learn() {
    PRINTLN("---------------------------");
    PRINTLN("Training starts...");
    PRINTLN("---------------------------");

    var smoothFactor = initialSF
    // Calculate gradient vector
    PRINTLN("w: ")
    w.foreach(x => PRINT(""+x+" "))
    while(smoothFactor >= stoppingSF){
      val dw = scaledSamples.map(rl => grad(rl, smoothFactor)).transpose.toArray.map(_.sum)
      // TODO: Tijdelijk hier gradient descent, vervangen door NonLinearConjugateGradient met Polak-Ribiere update
      // TODO: l_2-norm regularization term hier in verwerken
      w = (w, dw).zipped.map((x, y) => x+y)
      PRINTLN("w: ")
      w.foreach(x => PRINT(""+x+" "))
      smoothFactor = smoothFactor / 2
    }
    /*
        for(rl:RankList <- scaledSamples) {
          PRINTLN("grad(rl):")
          grad(rl).foreach(x => PRINT(""+x))

          val wEvaluated = (1 until rl.size).map(x => eval(rl.get(x)))
          val idx = Sorter.sort(wEvaluated.toArray, false)
          val wRanked = new RankList(rl, idx)
          val idealRanked = rl.getCorrectRanking

          val divisor = (0 until rl.size-1).map(j =>
            (0 until rl.size-1).map(i =>
              Math.exp(-(Math.pow(eval(wRanked.get(i)) - eval(idealRanked.get(j)), 2) / smoothFactor))
            ).reduceLeft(_+_)
          )

          /*val h = (0 until rl.size-1).map(j =>
            (0 until rl.size-1).map(i =>
              Math.exp(-(Math.pow(eval(wRanked.get(i)) - eval(idealRanked.get(j)), 2) / smoothFactor) / divisor(j))
            )
          )*/
          val Aq = (0 until rl.size-1).map(j =>
            (1/(Math.log(j+2)/Math.log(2))) * (0 until rl.size-1).map(i =>
              Math.pow(wRanked.get(i).getLabel, 2) * Math.exp(-(Math.pow(eval(wRanked.get(i)) - eval(idealRanked.get(j)), 2) / smoothFactor) / divisor(j))
            ).reduceLeft(_+_)
          ).reduceLeft(_+_)
          totalAq += Aq
    }*/

    //
    //)
    //val optimizer = new NonLinearConjugateGradientOptimizer(NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE, null)
  }

  override def eval(p:DataPoint):Double = {
    val weightedFeatures = (1 to features.length).map(x => p.getFeatureValue(x)*w(x-1) )
    return weightedFeatures.reduceLeft(_+_)
  }

  override def rank(rl:RankList):RankList = {
    val wEvaluated = (0 until rl.size).map(x => eval(rl.get(x)))
    val idx = Sorter.sort(wEvaluated.toArray, false)
    return new RankList(rl, idx)
  }

  def addToPseudoSolver(rl:RankList, a:ReshapeMatrix64F, b:ReshapeMatrix64F) = {
    var aRl = new DenseMatrix64F(rl.size, features.length)
    var bRl = new DenseMatrix64F(rl.size, 1)
    for(i <- 0 until rl.size){
      var xElem = List[Double]()
      val dp = rl.get(i)
      for(j <- 1 to features.length)
        aRl.set(i, j-1, dp.getFeatureValue(j).toDouble)
      bRl.set(i, 0, Math.pow(2, dp.getLabel)-1 )
    }
    CommonOps.insert(aRl,a,0,0)
    CommonOps.insert(bRl,b,0,0)
  }

  def scaleFeatures(samples:java.util.List[RankList], numFeatures:Int):Array[RankList] = {
    val rls = samples.asScala
    var mins:Array[Double] = new Array[Double](numFeatures)
    var maxs:Array[Double] = new Array[Double](numFeatures)

    // Pass 1: identify minimum and maximum value per feature
    for(i <- 0 until rls.size){
      val rl = rls(i)
      for(j <- 0 until rl.size){
        val dp = rl.get(j)
        for(k <- 1 to features.length){
          val f = dp.getFeatureValue(k)
          mins(k-1) = Math.min(mins(k-1), f)
          maxs(k-1) = Math.max(maxs(k-1), f)
        }
      }
    }

    // Pass 2: scale features
    for(i <- 0 until rls.size){
      val rl = rls(i)
      for(j <- 0 until rl.size){
        val dp = rl.get(j)
        for(k <- 1 to features.length){
          var newvalue = (0.0).toFloat
          if((maxs(k-1)-mins(k-1)) != 0)
            newvalue = ((dp.getFeatureValue(k)-mins(k-1))/(maxs(k-1)-mins(k-1))).toFloat
          dp.setFeatureValue(k, newvalue)
        }
      }
    }

    return rls.toArray
  }

  def grad(rl:RankList, sf:Float):Array[Float] = {
    return arrayByConst(2/sf,
      (0 until rl.size).map(j =>
        arrayByConst(
          D(j+1, rl) / Math.pow(E(j,rl,sf), 2).toFloat,
          (
            (
              (
                arrayByConst(
                  (0 until rl.size).map(i => (G(l(i,rl))*e(i,j,rl,sf))).reduceLeft(_+_), // Wat doet deze e_i hier! e hoort twee parameters te hebben
                  (0 until rl.size).map(p => arrayByConst(
                    e(p,j,rl,sf)*(f(p,rl)-f(d(j+1,rl),rl)),
                    X(p,rl))
                  ).transpose.toArray.map(_.sum)
                )
              ,
                arrayByConst(
                  E(j,rl,sf),
                  (0 until rl.size).map(p =>
                    arrayByConst(
                      e(p,j,rl,sf)*(f(p,rl)-f(d(j+1,rl),rl))*G(l(p,rl)),
                      X(p,rl))).transpose.toArray.map(_.sum)
                )
              ).zipped.map((x,y) => x-y)
            ,
              arrayByConst(
                E(j,rl,sf),
                (0 until rl.size).map(i =>
                  arrayByConst(
                    G(l(i,rl))*e(i,j,rl,sf)*f(i,rl),
                    X(d(j+1,rl),rl)
                  )
                ).transpose.toArray.map(_.sum)
              )
            ).zipped.map((x,y) => x+y)
          ,
            arrayByConst(
              (0 until rl.size).map(i => G(l(i,rl))*e(i,j,rl,sf)).reduceLeft(_+_) * (0 until rl.size).map(i => f(i,rl)*e(i,j,rl,sf)).reduceLeft(_+_),
              X(d(j+1,rl), rl)
            )
          ).zipped.map((x,y) => x-y)
        )
      ).transpose.toArray.map(_.sum)
    )
  }

  def adSum(ad: Array[Float]):Float = {
    var sum:Float = 0
    var i = 0
    while (i<ad.length) { sum += ad(i); i += 1 }
    sum
  }

  // TODO: Dit is wat raar, checken...
  def D(r:Int, rl:RankList):Float = {
    var D = (0).toFloat
    if (r <= k) {
      D = 1 / (Math.log(1 + r) / Math.log(2)).toFloat
      D = D / G(rl.getCorrectRanking.get(r).getLabel)
    }
    D //return value
  }

  /*
   * Returns feature vector of element p in RankList rl
   * @requires rl.size >= p > 0
   */
  def X(p:Int, rl:RankList):Array[Float] = {
    (1 to features.length).map(x => rl.get(p).getFeatureValue(x)).toArray
  }

  def G(l:Float):Float = {
    (Math.pow(2, l.toDouble) - 1).toFloat
  }

  def l(i:Int, rl:RankList):Float = {
    rl.get(i).getLabel
  }

  /*
   * Returns, for a given k, the sum of e(i,k) for all documents i in rl
   * @requires rl.size > k >= 0
   */
  def E(k:Int, rl:RankList, SmoothFactor:Float):Float = {
    (0 until rl.size).map(j => e(k,j,rl,SmoothFactor)).reduceLeft(_+_)
  }

  /*
   * Returns the gradient of indicator variable h, that represents the probability that document i is ranked at the j-th position
   * @requires rl.size > i >= 0
   * @requires rl.size >= j > 0
   * @ensures 1 >= result >= 0
   */
  def e(i:Int, j:Int, rl:RankList, smoothFactor:Float):Float = {
    //val ideal = rl.getCorrectRanking
    //val predicted = (0 until rl.size).map(x => eval(rl.get(x)))
    //val idx = Sorter.sort(predicted.toArray, false)
    Math.exp(- Math.pow( f(i,rl) -
      f(d(j+1,rl),rl) ,2) / smoothFactor ).toFloat
  }

  /*
   * Returns the index of the document ranked at position j in RankList rl
   * @requires j > 0 >= rl.size
   * @ensures rl.size > i >= 0
   */
  def d(j:Int, rl:RankList):Int = {
    val dp = rank(rl).get(j-1)
    var i = 0
      while (i < rl.size && !rl.get(i).equals(dp)) {
        i = i+1
      }
    i
  }

  /*
   * Returns the model prediction for element i in RankList rl
   * @requires rl.size > i >= 0
   */
  def f(i:Int, rl:RankList):Float = {
    eval(rl.get(i)).toFloat
  }

  def arrayByConst(const:Float, a:Array[Float]):Array[Float] = {
    a.map(x => x*const)
  }
}