package letor.serial;

import ciir.umass.edu.learning.{RankList, Ranker, DataPoint}
import ciir.umass.edu.utilities.Sorter
import scala.collection.JavaConverters._

/*
 * Implementation of SmoothRank
 * @Author Niek Tax
 */
class SmoothRank extends Ranker{
  //Parameters
  val initialSF                = Math.pow(2,  6).toFloat
  val stoppingSF               = Math.pow(2, -6).toFloat
  val initIterations           = 1
  val initEps                  = 0.0000001.toFloat // step size
  val k                        = 10
  val lambda                   = 0.01 // We chose it on the validation set in the set {10E−6, 10E−5, . . . , 10E2, 10E3}

  //Local variables
  var w:Array[Float]                = null
  var w0:Array[Float]               = null
  var scaledSamples:Array[RankList] = null

  override def init() {
    PRINT("Initializing... ")

    scaledSamples = scaleFeatures(samples, features.length)

    w = (0 until features.length).map(i => (1.toFloat/features.length)).toArray // uniform weight vector
    PRINTLN("Start iterations")
    PRINTLN("")
    for(i <- 1 to initIterations) {
      val dw = arrayByConst(initEps, scaledSamples.map(rl => (0 until rl.size - 1).map(dpi => (1 to features.length).map(f => rl.get(dpi).getFeatureValue(f) * (rl.get(dpi).getLabel - eval(rl.get(dpi)).toFloat))).transpose.toArray.map(_.sum)).transpose.toArray.map(_.sum))
      w = (w, dw).zipped.map(_ + _)
      PRINT("dw: ")
      dw.foreach(x => PRINT("" + x + " "))
      PRINTLN("")
      PRINT("w:  ")
      w.foreach(x => PRINT("" + x + " "))
      PRINTLN("")

      val docWiseLoss = scaledSamples.map(
        rl => (0 until rl.size - 1)
          .map(dpi => Math.pow(eval(rl.get(dpi)) - rl.get(dpi).getLabel, 2))
      )

      //    PRINT("docWiseLoss: ")
      //    docWiseLoss.foreach(x => PRINTLN(""+x+" "))
      //    PRINTLN("")

      val cost = docWiseLoss.map(
        q => if (q.isEmpty) 0 else q.reduceLeft(_ + _)
      ).reduceLeft(_ + _)
      PRINTLN("Cost: " + cost)
      PRINTLN("")
    }
    w0 = w
    PRINTLN("[DONE]")
  }

  override def learn() {
    PRINTLN("---------------------------");
    PRINTLN("Training starts...");
    PRINTLN("---------------------------");

    var smoothFactor = initialSF
    var d:Array[Float]  = null
    var g2:Array[Float] = null

    // Conjugate Gradient step size options
    val aOptions = Array(1.0,0.5,0.1,0.05,0.01,0.005,0.001).map(x => x.toFloat)

    // Step 1
    var g = scaledSamples.map(rl => grad(rl, smoothFactor)).transpose.toArray.map(_.sum)
    d = arrayByConst(-1, g)

    while(smoothFactor >= stoppingSF){
      // SmoothRank Annealing step
      smoothFactor = smoothFactor / 2

      // Backup w
      val w_backup = w
      var bestW    = w
      var minF     = Float.MaxValue

      // Step 2
      for(a <- aOptions){
        w = (w_backup, arrayByConst(a,d)).zipped.map(_+_)
        val thisF = F(smoothFactor)
        if(thisF < minF) {
          minF = thisF
          bestW = w
        }
      }

      // Step 3
      w = bestW
      PRINTLN("")
      PRINT("w:  ")
      w.foreach(x => PRINT(""+x+" "))

      // Step 4 + 5
      val g2 = arrayByConst(-1, scaledSamples.map(rl => grad(rl, smoothFactor)).transpose.toArray.map(_.sum))
      val b = (g2, (g2, g).zipped.map(_-_)).zipped.map(_*_).sum  / g.map(x => 2*x).sum
      d = (g2, arrayByConst(b, d)).zipped.map(_+_)
      g = g2
    }
  }

  override def eval(p:DataPoint):Double = {
    val weightedFeatures = (1 to features.length).map(x =>
      p.getFeatureValue(x) *
      w(x-1)
    )
    weightedFeatures.reduceLeft(_+_)
  }

  override def rank(rl:RankList):RankList = {
    val wEvaluated = (0 until rl.size).map(x => eval(rl.get(x)))
    val idx = Sorter.sort(wEvaluated.toArray, false)
    new RankList(rl, idx)
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

    rls.toArray
  }

  def F(sf:Float):Float = {
    var totalAq = 0.0.toFloat
    for(rl:RankList <- scaledSamples) {
      val wEvaluated = (1 until rl.size).map(x => eval(rl.get(x)))
      val idx = Sorter.sort(wEvaluated.toArray, false)
      val wRanked = new RankList(rl, idx)
      val idealRanked = rl.getCorrectRanking

      val divisor = (0 until rl.size-1).map(j =>
        (0 until rl.size-1).map(i =>
          Math.exp(-(Math.pow(eval(wRanked.get(i)) - eval(idealRanked.get(j)), 2) / sf))
        ).reduceLeft(_+_)
      )

      val Aq = (0 until rl.size-1).map(j =>
        (1/(Math.log(j+2)/Math.log(2))) * (0 until rl.size-1).map(i =>
          Math.pow(wRanked.get(i).getLabel, 2) * Math.exp(-(Math.pow(eval(wRanked.get(i)) - eval(idealRanked.get(j)), 2) / sf) / divisor(j))
        ).reduceLeft(_+_)
      ).reduceLeft(_+_).toFloat
      totalAq += Aq
    }
    // l2-norm term
    val l2 = Math.sqrt((w,w0).zipped.map(_-_).map(Math.pow(_,2)).reduceLeft(_+_))
    l2.toFloat - totalAq
  }

  def grad(rl:RankList, sf:Float):Array[Float] = {
    arrayByConst(2/sf,
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

  // Discount function
  def D(r:Int, rl:RankList):Float = {
    var D = (0).toFloat
    if (r <= k) {
      D = 1 / (Math.log(1 + r) / Math.log(2)).toFloat
      D = D/Z(rl)
    }
    D
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
   * Returns the DCG obtained with the best ranking
   */
  def Z(rl:RankList):Float = {
    val correct = rl.getCorrectRanking
    val maxIndex = Math.min(rl.size()-1,k-1)
    val z = (0 until maxIndex).map(i => correct.get(i).getLabel).reduceLeft(_+_)
    z
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