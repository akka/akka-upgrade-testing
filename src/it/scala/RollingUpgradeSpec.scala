import scala.language.postfixOps
import scala.sys.process._
import scala.util.control.NonFatal

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

class RollingUpgradeSpec extends AbstractRollingUpgradeSpec {
  import RollingUpgradeSpec._

  val akkaVersions = {
    Seq("2.6.4", "2.6.5") ++
    Option(System.getProperty("build.akka.26.snapshot")).toSeq
  }

  println("Running with versions : " + akkaVersions)

  var colour: Colour = Green

  def buildImages(): Unit = {
    akkaVersions.foreach { version =>
      val dVersion = dockerVersion(version)
      println(s"Building $version - docker version $dVersion")
      withClue(s"Failed to build version $version") {
        (s"./buildImage.sh $version" !) shouldEqual 0
      }
    }
  }

  "Rolling upgrade" should {

    "build images" in {
      buildImages()
    }

    "deploy an initial cluster then upgrade through all the versions" in {
      try {
        deploy(akkaVersions.head, colour, 3)
        assertAllReadyAndUpdated()
        logCheck()
      } catch {
        case NonFatal(e) =>
          printLogs()
          throw e
      }
    }

    akkaVersions.tail.foreach { version =>
      s"upgrade to version $version" in {
        try {
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
        } catch {
          case NonFatal(e) =>
            printLogs()
            throw e
        }
      }
    }
  }

  override def afterAll(): Unit = {
    system.terminate()
    s"kubectl delete deployment akka-upgrade-testing-$colour" !
  }
}
