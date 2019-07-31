import java.io.BufferedOutputStream
import java.io.FileOutputStream

import akka.cluster.protobuf.msg.ClusterMessages
import akka.cluster.protobuf.msg.ClusterMessages.ConfigCheck

object Main extends App {

  val address = ClusterMessages.Address.newBuilder().setHostname("localhost").setPort(1234).setSystem("system1").build()

  val uniqueAddress = ClusterMessages.UniqueAddress.newBuilder().setAddress(address).setUid(1).setUid2(2).build()

  val messages = Map(
    "join" -> ClusterMessages.Join.newBuilder().setNode(uniqueAddress).addRoles("dc-default").build().toByteArray,
    "init-join" -> ClusterMessages.InitJoin.newBuilder().setCurrentConfig("current-config").build().toByteArray,
    "welcome" -> ClusterMessages.Welcome
      .newBuilder()
      .setGossip(
        ClusterMessages.Gossip
          .newBuilder()
          .setOverview(ClusterMessages.GossipOverview.newBuilder().build())
          .setVersion(ClusterMessages.VectorClock.newBuilder().build())
          .build())
      .setFrom(uniqueAddress)
      .build()
      .toByteArray,
    "init-join-ack" -> ClusterMessages.InitJoinAck
      .newBuilder()
      .setConfigCheck(
        ClusterMessages.ConfigCheck
          .newBuilder()
          .setType(ConfigCheck.Type.IncompatibleConfig)
          .setClusterConfig("cats")
          .build())
      .setAddress(address)
      .build()
      .toByteArray)

  messages.foreach { msg =>
    val bos =
      new BufferedOutputStream(new FileOutputStream(s"messages/proto2/${msg._1}.out"))
    bos.write(msg._2)
    bos.close()
  }

}
