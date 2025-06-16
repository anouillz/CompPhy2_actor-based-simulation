package simulation

import scala.util.Random

case class Agent(id: Int, x: Double, y: Double, isCooperator: Boolean, points: Double = 0.0, isLeader: Boolean = false, impact: Double = 0.0, coolDown: Int = 0) {
  val size: Int = 10

  /** Play one prisoner dilemma round with another agent, updating this agent’s points. */
  def playRound(opponent: Agent, reward: Double, penalty: Double, suckerPay: Double, temptPay: Double): Agent = {
    val score = (isCooperator, opponent.isCooperator) match {
      case (true,  true ) => reward
      case (false, false) => penalty
      case (true,  false) => suckerPay
      case (false, true ) => temptPay
    }
    copy(points = points + score)
  }

  def move(speed: Double, worldSize: Double, rng: Random): Agent = {
    // helper to clamp a value into [min, max]
    def clamp(v: Double, min: Double, max: Double): Double =
      math.max(min, math.min(max, v))

    // pick a random direction
    val delta  = rng.nextDouble() * 2 * math.Pi
    val dx = math.cos(delta) * speed
    val dy = math.sin(delta) * speed

    // tentative new position
    val nx = x + dx
    val ny = y + dy

    // clamp into the world bounds
    val cx = clamp(nx, 0.0, worldSize)
    val cy = clamp(ny, 0.0, worldSize)

    // return moved agent
    copy(x = cx, y = cy)
  }


}
