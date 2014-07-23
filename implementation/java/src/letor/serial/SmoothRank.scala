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
  val smoothFactor             = 1
  val initIterations           = 10
  val initEps                  = 0.01 // step size
  val k                        = 10

  //Local variables
  var w:Array[Float] = null
  var scaledSamples:Array[RankList] = null

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
    w = X.getData.map(x => x.asInstanceOf[Float])


    PRINTLN("[DONE]")
  }

  override def learn() {
    PRINTLN("---------------------------");
    PRINTLN("Training starts...");
    PRINTLN("---------------------------");

    for(rl:RankList <- scaledSamples) {
      PRINTLN("grad(rl): "+grad(rl))
      /*val wEvaluated = (1 until rl.size).map(x => eval(rl.get(x)))
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
      totalAq += Aq*/
    }

    //
    //)
    //val optimizer = new NonLinearConjugateGradientOptimizer(NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE, null)
  }

  override def eval(p:DataPoint):Float = {
    val weightedFeatures = (1 until features.length).map(x => p.getFeatureValue(x)*w(x) )
    return weightedFeatures.reduceLeft[Float](_+_)
  }

  override def rank(rl:RankList):RankList = {
    val wEvaluated = (0 until rl.size-1).map(x => eval(rl.get(x)))
    val idx = Sorter.sort(wEvaluated.toArray, false)
    return new RankList(rl, idx)
  }

  def addToPseudoSolver(rl:RankList, a:ReshapeMatrix64F, b:ReshapeMatrix64F) = {
    var aRl = new DenseMatrix64F(rl.size(), features.length)
    var bRl = new DenseMatrix64F(rl.size(), 1)
    for(i <- 1 until rl.size()){
      var xElem = List[Double]()
      val dp = rl.get(i)
      for(j <- 1 until features.length)
        aRl.set(i-1, j-1, dp.getFeatureValue(j).asInstanceOf[Double])
      bRl.set(i-1, 0, Math.pow(2, dp.getLabel)-1 )
    }
    CommonOps.insert(aRl,a,0,0)
    CommonOps.insert(bRl,b,0,0)
  }

  def scaleFeatures(samples:java.util.List[RankList], numFeatures:Int):Array[RankList] = {
    val rls = samples.asScala
    var mins:Array[Double] = new Array[Double](numFeatures)
    var maxs:Array[Double] = new Array[Double](numFeatures)

    // Pass 1: identify minimum and maximum value per feature
    for(i <- 1 until rls.size){
      val rl = rls(i)
      for(j <- 1 until rl.size){
        val dp = rl.get(j)
        for(k <- 1 until features.length){
          val f = dp.getFeatureValue(k)
          mins(k) = Math.min(mins(k), f)
          maxs(k) = Math.max(maxs(k), f)
        }
      }
    }

    // Pass 2: scale features
    for(i <- 1 until rls.size){
      val rl = rls(i)
      for(j <- 1 until rl.size){
        val dp = rl.get(j)
        for(k <- 1 until features.length){
          var newvalue = 0.0.asInstanceOf[Float]
          if((maxs(k)-mins(k)) != 0)
            newvalue = ((dp.getFeatureValue(k)-mins(k))/(maxs(k)-mins(k))).asInstanceOf[Float]
          dp.setFeatureValue(k, newvalue)
        }
      }
    }

    return rls.toArray
  }

  def grad(rl:RankList):Array[Float] = {
    return (2/smoothFactor) * (1 until rl.size).map(j =>
      (D(j, rl) / Math.pow(E(j, rl), 2)) * (
        (1 until rl.size).map(i => (G(l(i,rl))*e(i,j,rl))).reduceLeft(_+_) * // Wat doet deze e_i hier! e hoort twee parameters te hebben
        (1 until rl.size).map(p => arrayByConst(e(p,j,rl)*(f(p,rl)-f(d(j,rl),rl)), X(p,rl))).reduceLeft(_+_) -
        E(j,rl)*(1 until rl.size).map(p => arrayByConst(e(p,j,rl)*(f(p,rl)-f(d(j,rl),rl))*G(l(p,rl)), X(p,rl))).reduceLeft(_+_) +
        E(j,rl)*(1 until rl.size).map(i => arrayByConst(G(l(i,rl))*e(i,j,rl)*f(i,rl), X(d(j,rl)))).reduceLeft(_+_) -
        arrayByConst((1 until rl.size).map(i => G(l(i,rl))*e(i,j,rl)).reduceLeft(_+_) *
        (1 until rl.size).map(i => f(i,rl)*e(i,j,rl)).reduceLeft(_+_), X(d(j,rl), rl))
      )
    ).reduceLeft(_+_)
  }

  // TODO: Dit is wat raar, checken...
  def D(r:Int, rl:RankList):Float = {
    var D = 0.asInstanceOf[Float]
    if (r <= k) {
      D = 1 / (Math.log(1 + r) / Math.log(2)).asInstanceOf[Float]
      D = D / G(rl.getCorrectRanking.get(r).getLabel)
    }
    D //return value
  }

  def X(p:Int, rl:RankList):Array[Float] = {
    (1 until features.length).map(x => rl.get(p).getFeatureValue(x)).toArray
  }

  def G(l:Float):Float = {
    (Math.pow(2, l.asInstanceOf[Double]) - 1).asInstanceOf[Float]
  }

  def l(i:Int, rl:RankList):Float = {
    rl.get(i).getLabel
  }

  def E(k:Int, rl:RankList):Float = {
    (1 until rl.size).map(x => e(k,x,rl)).reduceLeft(_+_)
  }

  def e(i:Int, j:Int, rl:RankList):Float = {
    val ideal = rl.getCorrectRanking
    val predicted = (1 until rl.size).map(x => eval(rl.get(x)))
    val idx = Sorter.sort(predicted.toArray, false)
    Math.exp(- Math.pow( f(i,rl) - f(d(j,rl),rl) ,2) / smoothFactor ).asInstanceOf[Float]
  }

  def d(j:Int, rl:RankList):Int = {
    val dp = rank(rl).get(j)
    var i = 1
      while (i < rl.size && !rl.get(i).equals(dp)) {
        i = i+1
      }
    i
  }

  def f(i:Int, rl:RankList):Float = {
    eval(rl.get(i))
  }

  def arrayByConst(const:Float, a:Array[Float]):Array[Float] = {
    a.map(x => x*const)
  }
}