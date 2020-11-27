import RollingUpgradeSpec.Colour
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, Uri }
import akka.stream.SystemMaterializer
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, CancelAfterFailure, Matchers, WordSpecLike }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.time.{ Seconds, Span }

import scala.collection.immutable
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.sys.process._
import scala.language.postfixOps

class AbstractRollingUpgradeSpec
    extends WordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually
    with Matchers
    with CancelAfterFailure {

  implicit val system = ActorSystem(
    "Test",
    ConfigFactory.parseString("""
    akka.actor.provider = local
    """))
  implicit val mat = SystemMaterializer(system).materializer

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(180, Seconds), interval = Span(5, Seconds))

  def dockerVersion(version: String): String = version.replace("+", "-")

  def deploy(version: String, colour: Colour, replicas: Int): Unit = {
    val dVersion = dockerVersion(version)
    println(s"Deploying version $dVersion")
    "cat deployment.yml" #| s"sed s/VERSION/$dVersion/g" #| s"sed s/COLOUR/${colour.name}/g" #| s"sed s/REPLICAS/$replicas/g" #| "kubectl apply -f -" !
  }

  // Logs that may happen during shutdown that aren't a problem
  val logExcludes = Set(
    "Connection attempt failed. Backing off new connection attempts", // happens during bootstrap
    "failed because of java.net.ConnectException: Connection refused",
    "Upstream failed, cause: Association$OutboundStreamStopQuarantinedSignal$",
    "Upstream failed, cause: StreamTcpException: Tcp command",
    "Upstream failed, cause: StreamTcpException: The connection closed with error: Broken pipe",
    "Restarting graph due to failure. stack_trace:",
    "Pool slot was shut down",
    "Outgoing request stream error",
    "The connection closed with error: Connection reset by peer"
    // all these happen when a node can't connect / communicate but they typically resolve them selves as long as the node goes ready
  )

  def url(): String = {
    ("minikube service --url upgrade" !!).trim.replace("* ", "")
  }

  def greenUrl(): String = {
    ("minikube service --url green" !!).trim.replace("* ", "")
  }

  def blueUrl(): String = {
    ("minikube service --url blue" !!).trim.replace("* ", "")
  }

  def versions() = {
    val versionsUri = s"${url()}/versions"
    println(s"using versions uri |$versionsUri|")
    Http(system)
      .singleRequest(HttpRequest(uri = Uri(versionsUri)))
      .futureValue
      .entity
      .toStrict(6.seconds)(mat)
      .futureValue
      .data
      .utf8String
  }

  def testDdata() = {
    val ddataUri = s"${url()}/test/ddata"
    println(s"using ddata uri |$ddataUri|")
    Http(system)
      .singleRequest(HttpRequest(uri = Uri(ddataUri)))
      .futureValue
      .entity
      .toStrict(6.seconds)(mat)
      .futureValue
      .data
      .utf8String
  }

  def testSharding() = {
    val shardingUri = s"${url()}/test/sharding"
    println(s"using sharding uri |$shardingUri|")
    Http(system)
      .singleRequest(HttpRequest(uri = Uri(shardingUri)))
      .futureValue
      .entity
      .toStrict(2.seconds)(mat)
      .futureValue
      .data
      .utf8String
  }

  def logCheck(): Unit = {
    val nodes = ("kubectl get pods" !!).split("\\n").toList.tail.map(_.takeWhile(_ != ' '))
    val logs: immutable.Seq[(String, Array[String])] =
      nodes.map(pod => (pod, (s"kubectl logs $pod" !!).split("\\n")))
    logs.foreach {
      case (node, ls) =>
        val errorAndWarnings = ls.filter(line =>
          (line.contains("ERROR") || line.contains("WARN")) && logExcludes.forall(ex => !line.contains(ex)))
        withClue(s"Warnings and errors found on node $node. All lines: \n ${ls
          .mkString("\n")} \n Offending lines: \n ${errorAndWarnings.mkString("\n")}") {
          errorAndWarnings.nonEmpty shouldEqual false
        }
    }
  }

  def printLogs(): Unit = {
    def getLogs(pod: String): String = {
      try {
        s"kubectl logs $pod" !!
      } catch {
        case NonFatal(t) => s"unable to get logs for pod $pod. Reason: ${t.getMessage}"
      }
    }
    val nodes = ("kubectl get pods" !!).split("\\n").toList.tail.map(_.takeWhile(_ != ' '))
    val logs: immutable.Seq[(String, Array[String])] =
      nodes.map(pod => (pod, (getLogs(pod)).split("\\n")))
    logs.foreach {
      case (node, ls) =>
        println(s"# Log on node [$node]:\n${ls.mkString("\n")}")
    }
  }

  def status(): Seq[String] = ("kubectl get pods" !!).split("\\n").tail.toList

  def assertAllReadyAndUpdated(duration: FiniteDuration = 3.minutes): Unit = {
    withClue(status().mkString("\n")) {
      eventually {
        val s = status()
        println(s"Current status: ${s.mkString("\n")}")
        s.forall(_.contains("1/1")) shouldEqual true
        s.forall(_.contains("Running")) shouldEqual true
        println("All ready \n" + s.mkString("\n"))
      }
      logCheck()
    }

    println(versions())

    testDdata() shouldEqual "OK"

    // TODO pending
    println(testSharding())
  }

}
