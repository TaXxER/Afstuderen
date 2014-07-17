/*for (i <- 1 to ITERATIONS) {
  val gradient = sparkContext.accumulator(spark.examples.Vector.zeros(dim))
  val loss = sparkContext.accumulator(0.0)
  for (q <- queries) {
    val expRelScores = q.relScores.map(y => math.exp(beta*y.toDouble))
    val ourScores = q.docFeatures.map(x => w dot x); val expOurScores = ourScores.map(z => math.exp(z))
    val sumExpRelScores = expRelScores.reduce(_ + _); val sumExpOurScores = expOurScores.reduce(_ + _)
    val P_y = expRelScores.map(y => y/sumExpRelScores); val P_z = expOurScores.map(z => z/sumExpOurScores)
    var lossForAQuery = 0.0; var gradientForAQuery = spark.examples.Vector.zeros(dim)
    for (j <- 0 to q.relScores.length-1) {
      gradientForAQuery += (q.docFeatures(j) * (P_z(j) - P_y(j)))
      lossForAQuery += -P_y(j) * math.log(P_z(j))
    }
    gradient += gradientForAQuery; loss += lossForAQuery
  }
  w -= gradient.value * stepSize
}*/