import java.nio.file.Files
import java.nio.file.Paths

import akka.cluster.protobuf.msg.ClusterMessages
import org.scalatest.Matchers
import org.scalatest.WordSpec

class Proto3CanParseProto2Spec extends WordSpec with Matchers {

  def load(name: String): Array[Byte] = {
    Files.readAllBytes(Paths.get(s"messages/proto2/$name.out"))
  }

  "Proto2 bytes -> Proto3 classes" should {
    "join" in {
      val join = ClusterMessages.Join.parseFrom(load("join"))
      join.getNode.getUid shouldEqual 1
      join.getNode.getUid2 shouldEqual 2
    }

    "init join" in {
      val initJoin = ClusterMessages.InitJoin.parseFrom(load("init-join"))
      initJoin.getCurrentConfig shouldEqual "current-config"
    }

    "init join ack" in {
      val initJoinAck =
        ClusterMessages.InitJoinAck.parseFrom(load("init-join-ack"))
      initJoinAck.getAddress.getHostname shouldEqual "localhost"
    }

    "welcome" in {
      val welcome = ClusterMessages.Welcome.parseFrom(load("welcome"))
      welcome.getFrom.getAddress.getHostname shouldEqual "localhost"
    }
  }

}
