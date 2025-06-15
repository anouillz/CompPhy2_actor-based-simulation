package simulation

case class World(agents: Seq[Agent],
                  worldSize: Double,
                  interactionRadius: Double,
                  cities: Seq[City]
                )
