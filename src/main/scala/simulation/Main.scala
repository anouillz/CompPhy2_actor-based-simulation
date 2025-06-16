package simulation

import scala.util.Random
import java.io.{File, PrintWriter}
import scala.util.Using

object Main {

  // === Tweakable parameters ===
  val nAgents           = 1500                     // total agents
  val coopRatio         = 0.6                      // 60% of all agents start as cooperators
  val worldSize         = 100.0                    // world is 100×100
  val interactionRadius = 1.0                      // who counts as your “neighbor”
  val cities: Seq[City] = Seq(
    City(20, 20, 15),   // city centered at (20,20) with radius 15
    City(80, 20, 12),   // city at top‐right
    City(50, 75, 18)    // big city near bottom‐middle
  )
  val nSteps            = 200                      // how many rounds to run
  val speed             = 0.5                      // how far each agent moves per step
  val temptation        = 1.5                      // T payoff for defecting on a cooperator
  val adoptionChance    = 0.8                      // base probability of copying a better neighbor
  val outputFile        = "output/simulation.json" // where to dump the JSON
  // ==============================

  def main(args: Array[String]): Unit = {
    val rng = new Random()

    val coopCityFrac   = 0.8   // 80% of cooperators in cities
    val defectCityFrac = 0.1   // 10% of defectors in cities

    var world = Simulation.initializeWorld(
      nAgents,
      coopRatio,
      coopCityFrac,
      defectCityFrac,
      worldSize,
      interactionRadius,
      cities,
      rng
    )

    //Run the simulation for nSteps, collecting snapshots
    val history = (1 to nSteps).foldLeft(List(world.agents)) { (hist, _) =>
      world = Simulation.step(world, speed, adoptionChance, temptation, rng)
      world.agents :: hist
    }.reverse

    // export to JSON
    val jsonFrames = history.map { frame =>
      val agentsJson = frame.map { a =>
        s"""{"id":${a.id},"x":${a.x},"y":${a.y},"strategy":"${if(a.isCooperator)"C" else "D"}"}"""
      }.mkString("[", ",", "]")
      agentsJson
    }.mkString("[\n", ",\n", "\n]")

    val file = new File(outputFile)
    file.getParentFile.mkdirs()
    Using.resource(new PrintWriter(file))(_.write(jsonFrames))
    println(s"Done: wrote ${history.size} frames to $outputFile")
  }
}
