import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
import sys.process._
import scala.language.postfixOps
import scala.concurrent.duration._

object Colour {
  def next(current: Colour): Colour = current match {
    case Green => Blue
    case _ => Green
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

// TODO take in from command line and include the latest nightly
val akkaVersions = Seq("2.5.23", "2.6-SNAPSHOT")
//val akkaVersions = Seq("2.5.23")

def deploy(version: String, colour: Colour , replicas: Int): Unit = {
  "cat deployment.yml" #| s"sed s/VERSION/$version/g" #| s"sed s/COLOUR/${colour.name}/g" #| s"sed s/REPLICAS/$replicas/g" #| "kubectl apply -f -" !
}

// Logs that may happen during shutdown that aren't a problem
val logExcludes = Set(
  "Connection attempt failed. Backing off new connection attempts", // happens during bootstrapo
  "failed because of java.net.ConnectException: Connection refused",
  "Upstream failed, cause: Association$OutboundStreamStopQuarantinedSignal$",
  "Upstream failed, cause: StreamTcpException: Tcp command",
  "Restarting graph due to failure. stack_trace:"
  // all these happen when a node can't connect / communicate but they typically resolve them selves as long as the node goes ready
)

def logCheck(): Unit = {
  val nodes = ("kubectl get pods" !!)
    .split("\\n")
    .toList
    .tail
    .map(_.takeWhile(_ != ' '))
  println("Nodes: " + nodes)
  val logs: immutable.Seq[(String, Array[String])] =
    nodes.map(pod => (pod, (s"kubectl logs $pod" !!).split("\\n")))
  logs.foreach {
    case (node, ls) =>
      val errorAndWarnings = ls.filter(
        line =>
          line.contains("ERROR") || line.contains("WARN") && logExcludes
            .forall(ex => !line.contains(ex))
      )
      if (errorAndWarnings.nonEmpty) {
        println(
          s"Warnings and errors found on node $node. \n" + errorAndWarnings
            .mkString("\n")
        )
        System.exit(-1)
      }
  }
}

def assertAllReadyAndUpdated(duration: FiniteDuration = 3.minutes): Unit = {
  val deadline = Deadline.now + duration
  def check(): Unit = {
    val status = ("kubectl get pods" !!).split("\\n").tail.toList
    if (status.forall(_.contains("1/1")) && status.forall(
          _.contains("Running")
        )) {
      logCheck()
      println("All ready \n" + status.mkString("\n"))
    } else {
      if (deadline.hasTimeLeft()) {
        println("Pods not ready. Trying again. \n " + status.mkString("\n"))
        Thread.sleep((duration / 10).toMillis)
        check()
      } else {
        println("Deadline has passed. \n" + status.mkString("\n"))
        System.exit(-1)
      }
    }
  }

  check()
}

def buildImages(): Unit = {
  println("Building images")
  akkaVersions foreach { version =>
    println(s"Building $version")
    s"sbt -Doverride.akka.version=$version docker:publishLocal" !
  }
  "docker images" #| "head" !
}


var colour: Colour = Green

//buildImages()

println(s"Creating initial cluster with ${akkaVersions.head}")
deploy(akkaVersions.head, colour, 3)

assertAllReadyAndUpdated()

logCheck()

akkaVersions.tail foreach { version =>
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

s"kubectl delete deployment akka-upgrade-testing-$colour" !



