import akka.http.javadsl.model.StatusCodes
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, Uri }

import scala.language.postfixOps
import scala.sys.process._
import scala.util.control.NonFatal
import scala.concurrent.duration._

class RollingUpgradeDuringFullShutdownSpec extends AbstractRollingUpgradeSpec {

  implicit val ec = system.dispatcher

  import RollingUpgradeSpec._

  val beforeFeature = "2.6.10"
  val afterFeature = "2.6.10+83-c2c44b53"

  val beforeFeatureColour: Colour = Blue
  val afterFeatureColour: Colour = Green

  def buildImages(): Unit = {
    Set(beforeFeature, afterFeature).foreach { version =>
      val dVersion = dockerVersion(version)
      println(s"Building $version - docker version $dVersion")
      withClue(s"Failed to build version $version") {
        (s"./buildImage.sh $version" !) shouldEqual 0
      }
    }
  }

  def prepareForShutdown(url: String) = {
    val shutdownUri = s"${url}/cluster/prepare-for-shutdown"
    println(s"using shutdown uri |$shutdownUri|")
    Http(system)
      .singleRequest(HttpRequest(uri = Uri(shutdownUri)))
      .map { response =>
        response.status match {
          case StatusCodes.OK => response
          case other =>
            throw new RuntimeException("Unexpected status code: " + other)
        }
      }
      .futureValue
      .entity
      .toStrict(2.seconds)(mat)
      .futureValue
      .data
      .utf8String
  }

  "Rolling upgrade with prepare shutdown" should {

    "build images" in {
      // FIXME put back, just to speed up local running
      buildImages()
    }

    "deploy an initial cluster then upgrade through all the versions" in {
      try {
        deploy(beforeFeature, beforeFeatureColour, 3)
        assertAllReadyAndUpdated()
        logCheck()
      } catch {
        case NonFatal(e) =>
          printLogs()
          throw e
      }
    }

    s"upgrade to version $afterFeature" in {
      val version = afterFeature
      try {
        println(s"upgrading to version $version")
        println(s"Adding one node of the new version $version")
        deploy(version, afterFeatureColour, 1)
        assertAllReadyAndUpdated()

        println(s"2 of each version")
        deploy(beforeFeature, beforeFeatureColour, 2)
        deploy(version, afterFeatureColour, 2)
        assertAllReadyAndUpdated()

        // now mark them all as ready for shutdown, part of the cluster understands this and part does not
        // the request must go to a node that understands it
        println("Marking for shutdown")
        println(prepareForShutdown(greenUrl()))

        Thread.sleep(2000)
        println(versions())

        println("Removing old deployment")
        s"kubectl delete deployment akka-upgrade-testing-$beforeFeatureColour" !
        s"kubectl delete deployment akka-upgrade-testing-$afterFeatureColour" !

        assertAllReadyAndUpdated()
      } catch {
        case NonFatal(e) =>
          printLogs()
          throw e
      }
    }
  }

}
