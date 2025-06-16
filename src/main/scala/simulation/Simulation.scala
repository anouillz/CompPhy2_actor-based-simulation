package simulation

import scala.util.Random
import scala.math._

object Simulation {

  private val R = 3.0;
  private val P = 1.0;
  private val S = 0.0

  private def distance(a: Agent, b: Agent): Double =
    hypot(a.x - b.x, a.y - b.y)

  private def getNeighbors(a: Agent, all: Seq[Agent], r: Double): Seq[Agent] =
    all.filter(o => o.id != a.id && distance(a, o) <= r)

  private def isInCity(x: Double, y: Double, cities: Seq[City]): Boolean =
    cities.exists { c =>
      val dx = x - c.x; val dy = y - c.y
      dx*dx + dy*dy <= c.radius*c.radius
    }

  def initializeWorld(nAgents: Int, coopRatio: Double, coopCityFrac: Double, defectCityFrac: Double, worldSize: Double, interactionRadius: Double, cities: Seq[City], rng: Random): World = {
    val totalCoop   = (coopRatio   * nAgents).round.toInt
    val totalDefect = nAgents - totalCoop

    val coopInCity    = (coopCityFrac    * totalCoop).round.toInt
    val coopOutCity   = totalCoop - coopInCity

    val defectInCity  = (defectCityFrac * totalDefect).round.toInt
    val defectOutCity = totalDefect - defectInCity

    var idGen = 0

    def sampleInside = {
      val City(cx, cy, r) = cities(rng.nextInt(cities.length))
      val θ    = rng.nextDouble() * 2*Pi
      val dist = sqrt(rng.nextDouble()) * r
      (cx + cos(θ)*dist, cy + sin(θ)*dist)
    }

    val coopsCity = Seq.fill(coopInCity) {
      val (x,y) = sampleInside
      val a = Agent(idGen, x, y, isCooperator = true)
      idGen += 1; a
    }
    val coopsCountry = Seq.fill(coopOutCity) {
      var x, y = 0.0
      do { x = rng.nextDouble()*worldSize; y = rng.nextDouble()*worldSize }
      while (isInCity(x,y,cities))
      val a = Agent(idGen, x, y, isCooperator = true)
      idGen += 1; a
    }
    val defsCity = Seq.fill(defectInCity) {
      val (x,y) = sampleInside
      val a = Agent(idGen, x, y, isCooperator = false)
      idGen += 1; a
    }
    val defsCountry = Seq.fill(defectOutCity) {
      var x, y = 0.0
      do { x = rng.nextDouble()*worldSize; y = rng.nextDouble()*worldSize }
      while (isInCity(x,y,cities))
      val a = Agent(idGen, x, y, isCooperator = false)
      idGen += 1; a
    }

    World(coopsCity ++ coopsCountry ++ defsCity ++ defsCountry, worldSize, interactionRadius, cities)
  }

  def step(
            world: World,
            speed: Double,
            adoptionChance: Double,
            temptation: Double,
            rng: Random
          ): World = {

    // 1) move
    val moved = world.agents.map(_.move(speed, world.worldSize, rng))

    // 2) play
    val scored = moved.map { a =>
      getNeighbors(a, moved, world.interactionRadius)
        .foldLeft(a)((acc, opp) => acc.playRound(opp, R, P, S, temptation))
    }

    // 3) adopt
    val updated = scored.map { a =>
      getNeighbors(a, scored, world.interactionRadius).maxByOption(_.points) match {
        case Some(best) if best.points > a.points && rng.nextDouble() < adoptionChance =>
          a.copy(isCooperator = best.isCooperator)
        case _ =>
          a
      }
    }

    // 4) reset points
    val reset = updated.map(a => a.copy(points = 0.0, coolDown = math.max(0, a.coolDown-1)))
    world.copy(agents = reset)
  }

  // count groups of cooperators
  def countCooperatorClusters(world: World): Int = {
    // Build index of cooperator agents
    val coops = world.agents.filter(_.isCooperator)
    val n      = coops.length
    // Precompute neighbor‐within‐radius relationships
    val adj = Array.fill(n)(collection.mutable.ListBuffer.empty[Int])
    for (i <- 0 until n; j <- i+1 until n) {
      if (hypot(coops(i).x - coops(j).x, coops(i).y - coops(j).y) <= world.interactionRadius) {
        adj(i) += j
        adj(j) += i
      }
    }
    // DFS to count connected components
    val seen = Array.fill(n)(false)
    var count = 0
    def dfs(i: Int): Unit = {
      seen(i) = true
      for (j <- adj(i) if !seen(j)) dfs(j)
    }
    for (i <- 0 until n if !seen(i)) {
      count += 1
      dfs(i)
    }
    count
  }

}
