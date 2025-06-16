package simulation

import scala.util.{Try, Random}
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.chart.{LineChart, NumberAxis, XYChart}
import scalafx.scene.layout.{BorderPane, VBox, HBox}
import scalafx.scene.paint.Color
import scalafx.animation.AnimationTimer


object Simulation_interface extends JFXApp3 {

  private val worldSize = 100.0
  private var world:       World             = _
  private var history:     Seq[Seq[Agent]]   = Seq.empty
  private var currentStep: Int               = 0
  private val rng                             = new Random()
  private var running: Boolean                = false

  override def start(): Unit = {

    // ─── TEXT FIELDS ──────────────────────────────────────────────────────────────
    def mkTF(prompt: String) = new TextField {
      promptText = prompt
      prefColumnCount = prompt.length
    }

    val tfAgents    = mkTF("100–5000")
    val tfCoopRatio = mkTF("0.0–1.0")
    val tfCoopCity  = mkTF("0.0–1.0")
    val tfDefCity   = mkTF("0.0–1.0")
    val tfSteps     = mkTF("10–1000")
    val tfRadius    = mkTF("0.1–5.0")
    val tfSpeed     = mkTF("0.1–2.0")
    val tfTempt     = mkTF("1.0–5.0")
    val tfAdopt     = mkTF("0.0–1.0")

    val btnStart   = new Button("Start")
    val btnRestart = new Button("Restart")
    val btnPause   = new Button("Pause")
    val btnExport  = new Button("Export CSV")
    val lblStep    = new Label("Step: 0")

    // ─── CANVAS & CHART ───────────────────────────────────────────────────────────
    val canvasSize = 500
    val canvas     = new Canvas(canvasSize, canvasSize)
    val gc: GraphicsContext = canvas.graphicsContext2D

    val timeAxis    = NumberAxis("Step",    0, 100, 10)
    val percentAxis = NumberAxis("Percent", 0, 100, 10)
    val chart       = new LineChart[Number, Number](timeAxis, percentAxis)
    val seriesC     = new XYChart.Series[Number, Number] { name = "Cooperators" }
    val seriesD     = new XYChart.Series[Number, Number] { name = "Defectors" }
    chart.getData.addAll(seriesC.delegate, seriesD.delegate)
    seriesC.delegate.getNode.setStyle("-fx-stroke: green;")
    seriesD.delegate.getNode.setStyle("-fx-stroke: red;")

    // ─── LAYOUT ─────────────────────────────────────────────────────────────────
    val controls = new VBox {
      spacing = 6
      children = Seq(
        new HBox(4) { children = Seq(new Label("Agents:"),         tfAgents) },
        new HBox(4) { children = Seq(new Label("Coop Ratio:"),     tfCoopRatio) },
        new HBox(4) { children = Seq(new Label("Coop in Cities:"), tfCoopCity) },
        new HBox(4) { children = Seq(new Label("Defect in Cities:"), tfDefCity) },
        new HBox(4) { children = Seq(new Label("Steps:"),          tfSteps) },
        new HBox(4) { children = Seq(new Label("Radius:"),         tfRadius) },
        new HBox(4) { children = Seq(new Label("Speed:"),          tfSpeed) },
        new HBox(4) { children = Seq(new Label("Temptation:"),     tfTempt) },
        new HBox(4) { children = Seq(new Label("Adoption %:"),     tfAdopt) },
        new HBox(8) { children = Seq(btnStart, btnRestart, btnPause, btnExport, lblStep) }
      )
    }

    stage = new PrimaryStage {
      title = "Adoption of Rules in Societies"
      scene = new Scene(canvasSize + 400, canvasSize + 400) {
        root = new BorderPane {
          left   = controls
          center = canvas
          bottom = chart
        }
      }
    }

    // ─── PARSE & CLAMP HELPER ────────────────────────────────────────────────────
    def parseOr[A](tf: TextField, lo: A, hi: A, f: String => A)(implicit N: Numeric[A]): A = {
      Try(f(tf.text.value)).toOption
        .map { v =>
          if (N.lt(v, lo)) lo
          else if (N.gt(v, hi)) hi
          else v
        }
        .getOrElse(lo)
    }

    // ─── INITIALIZE / RESTART ────────────────────────────────────────────────────
    def initSimulation(): Unit = {
      val nA             = parseOr(tfAgents,    100,   5000, _.toInt)
      val coopRatio      = parseOr(tfCoopRatio, 0.0,   1.0,  _.toDouble)
      val coopCityFrac   = parseOr(tfCoopCity,  0.0,   1.0,  _.toDouble)
      val defectCityFrac = parseOr(tfDefCity,   0.0,   1.0,  _.toDouble)
      val steps          = parseOr(tfSteps,     10,    1000, _.toInt)
      val radius         = parseOr(tfRadius,    0.1,   5.0,  _.toDouble)

      timeAxis.upperBound = steps
      timeAxis.tickUnit   = math.max(1, steps/10)

      world = Simulation.initializeWorld(
        nA,
        coopRatio,
        coopCityFrac,
        defectCityFrac,
        worldSize,
        radius,
        Main.cities,
        rng
      )

      history     = Seq(world.agents)
      currentStep = 0
      lblStep.text = "Step: 0"
      seriesC.data().clear(); seriesD.data().clear()
      running = true
      btnPause.text = "Pause"
    }

    btnStart.onAction   = _ => initSimulation()
    btnRestart.onAction = _ => initSimulation()

    // ─── PAUSE / RESUME ──────────────────────────────────────────────────────────
    btnPause.onAction = _ => {
      running = !running
      btnPause.text = if (running) "Pause" else "Resume"
    }

    // ─── EXPORT CSV ─────────────────────────────────────────────────────────────
    btnExport.onAction = _ =>
      if (history.nonEmpty) {
        import java.io.File
        import java.io.PrintWriter
        import scala.util.Using

        // 1) collect current UI parameter values
        val nA    = parseOr(tfAgents,    100, 5000, _.toInt)
        val coopR = parseOr(tfCoopRatio, 0.0,   1.0, _.toDouble)
        val coopC = parseOr(tfCoopCity,  0.0,   1.0, _.toDouble)
        val defC  = parseOr(tfDefCity,   0.0,   1.0, _.toDouble)
        val steps = parseOr(tfSteps,     10,  1000, _.toInt)
        val rad   = parseOr(tfRadius,   0.1,   5.0, _.toDouble)
        val spd   = parseOr(tfSpeed,    0.1,   2.0, _.toDouble)
        val temp  = parseOr(tfTempt,    1.0,   5.0, _.toDouble)
        val adop  = parseOr(tfAdopt,    0.0,   1.0, _.toDouble)

        // 2) open CSV and write
        new File("output").mkdirs()
        Using.resource(new PrintWriter(new File("output/simulation-data.csv"))) { w =>
          // metadata comment
          w.println(f"# nAgents=$nA, coopRatio=$coopR%.2f, coopCityFrac=$coopC%.2f, " +
            f"defectCityFrac=$defC%.2f, steps=$steps, radius=$rad%.2f, " +
            f"speed=$spd%.2f, temptation=$temp%.2f, adoptionChance=$adop%.2f")

          // column header
          w.println("step,id,x,y,strategy")

          // one row per agent per timestep
          history.zipWithIndex.foreach { case (agentsAtStep, step) =>
            agentsAtStep.foreach { a =>
              val strat = if (a.isCooperator) "C" else "D"
              w.println(s"$step,${a.id},${a.x},${a.y},$strat")
            }
          }
        }
      }


    // ─── ANIMATION LOOP ───────────────────────────────────────────────────────────
    val timer = AnimationTimer { _ =>
      if (running && world != null && currentStep < parseOr(tfSteps, 10, 1000, _.toInt)) {
        world = Simulation.step(
          world,
          speed          = parseOr(tfSpeed, 0.1, 2.0,   _.toDouble),
          adoptionChance = parseOr(tfAdopt, 0.0, 1.0,   _.toDouble),
          temptation     = parseOr(tfTempt, 1.0, 5.0,   _.toDouble),
          rng
        )
        history :+= world.agents
        currentStep += 1
        lblStep.text = s"Step: $currentStep"

        // update chart
        val pct = 100.0 * world.agents.count(_.isCooperator) / world.agents.size
        val d1 = new javafx.scene.chart.XYChart.Data[Number, Number](currentStep, pct)
        val d2 = new javafx.scene.chart.XYChart.Data[Number, Number](currentStep, 100 - pct)
        seriesC.delegate.getData.add(d1)
        seriesD.delegate.getData.add(d2)

        // redraw canvas
        val scale = canvasSize / world.worldSize
        gc.fill = Color.White; gc.fillRect(0, 0, canvasSize, canvasSize)
        gc.stroke = Color.Black
        world.cities.foreach { case City(cx, cy, r) =>
          val px = (cx - r) * scale; val py = (cy - r) * scale; val pw = 2 * r * scale
          gc.strokeOval(px, py, pw, pw)
        }
        world.agents.foreach { a =>
          val sx = a.x * scale; val sy = a.y * scale
          gc.fill = if (a.isCooperator) Color.Green else Color.Red
          gc.fillOval(sx - 2, sy - 2, 6, 6)
        }
      }
    }
    timer.start()
  }
}
