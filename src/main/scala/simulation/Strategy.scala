package simulation

trait Strategy

case object Cooperate extends Strategy {
  override def toString: String = "C"
}

case object Defect extends Strategy {
  override def toString: String = "D"
}
