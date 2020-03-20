import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.CancelAfterFailure
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Seconds
import org.scalatest.time.Span

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
import sys.process._
import scala.language.postfixOps
import scala.concurrent.duration._

object RollingUpgradeSpec {
  object Colour {
    def next(current: Colour): Colour = current match {
      case Green => Blue
      case _     => Green
    }
  }

  sealed trait Colour {
    def name: String
    override def toString = name

  }
  case object Green extends Colour {
    override def name = "green"
  }
  case object Blue extends Colour {
    override def name = "blue"
  }

}

class RollingUpgradeSpec
    extends WordSpec
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually
    with Matchers
    with CancelAfterFailure {
  import RollingUpgradeSpec._

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(180, Seconds), interval = Span(5, Seconds))

  val akkaVersions = {
    Seq("2.5.23", "2.5.29") ++
    Option(System.getProperty("build.akka.25.snapshot")).toSeq ++
    Seq("2.6.4")++
    Option(System.getProperty("build.akka.26.snapshot")).toSeq
  }

  println("Running with versions : " + akkaVersions)

 def dockerVersion(version: String): String = version.replace("+", "-")

  def deploy(version: String, colour: Colour, replicas: Int): Unit = {
    val dVersion = dockerVersion(version)
    println(s"Deploying version $dVersion")
    "cat deployment.yml" #| s"sed s/VERSION/$dVersion/g" #| s"sed s/COLOUR/${colour.name}/g" #| s"sed s/REPLICAS/$replicas/g" #| "kubectl apply -f -" !
  }

// Logs that may happen during shutdown that aren't a problem
  val logExcludes = Set(
    "Connection attempt failed. Backing off new connection attempts", // happens during bootstrapo
    "failed because of java.net.ConnectException: Connection refused",
    "Upstream failed, cause: Association$OutboundStreamStopQuarantinedSignal$",
    "Upstream failed, cause: StreamTcpException: Tcp command",
    "Restarting graph due to failure. stack_trace:",
    "Pool slot was shut down",
    "The connection closed with error: Connection reset by peer"
    // all these happen when a node can't connect / communicate but they typically resolve them selves as long as the node goes ready
  )

  def url(): String = {
    ("minikube service --url upgrade" !!).trim.replace("* ", "")
  }

  def versions() = {
    val versionsUri = s"${url()}/versions"
    println(s"using versions uri |$versionsUri|")
    Http(system)
      .singleRequest(HttpRequest(uri = Uri(versionsUri)))
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
        val errorAndWarnings = ls.filter(line => (line.contains("ERROR") || line.contains("WARN")) && logExcludes.forall(ex => !line.contains(ex)))
        withClue(s"Warnings and errors found on node $node. All lines: \n ${ls
          .mkString("\n")} \n Offending lines: \n ${errorAndWarnings.mkString("\n")}") {
          errorAndWarnings.nonEmpty shouldEqual false
        }
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
  }

  def buildImages(): Unit = {
    akkaVersions.foreach { version =>
      val dVersion = dockerVersion(version)
      println(s"Building $version - docker version $dVersion")
      withClue(s"Failed to build version $version") {
        (s"./buildImage.sh $version" !) shouldEqual 0
      }
    }
  }

  var colour: Colour = Green


  "Rolling upgrade" should {

    "build images" in {
      buildImages()
    }

    "deploy an initial cluster then upgrade through all the versions" in {
      deploy(akkaVersions.head, colour, 3)
      assertAllReadyAndUpdated()
      logCheck()
    }

    akkaVersions.tail.foreach { version =>
      s"upgrade to version $version" in {
        println(s"upgrading to version $version")
        val nextColour = Colour.next(colour)
        println(s"Adding one node of the new version $version")
        deploy(version, nextColour, 1)
        assertAllReadyAndUpdated()

        println(s"2 of each version")
        deploy(version, colour, 2)
        deploy(version, nextColour, 2)
        assertAllReadyAndUpdated()

        println(s"3 of new, 1 of old")
        deploy(version, colour, 1)
        deploy(version, nextColour, 3)
        assertAllReadyAndUpdated()

        println("Removing old deployment")
        s"kubectl delete deployment akka-upgrade-testing-$colour" !

        assertAllReadyAndUpdated()
        colour = nextColour
      }
    }
  }

  override def afterAll(): Unit = {
    system.terminate()
    s"kubectl delete deployment akka-upgrade-testing-$colour" !
  }
}
