import java.io.BufferedOutputStream
import java.io.FileOutputStream

import akka.cluster.protobuf.msg.ClusterMessages
import akka.cluster.protobuf.msg.ClusterMessages.ConfigCheck
import com.google.protobuf.AbstractMessageLite
import com.google.protobuf.GeneratedMessage

object Main extends App {

  val messages = Map(
    "config-check" -> ClusterMessages.ConfigCheck
      .newBuilder()
      .setType(ConfigCheck.Type.IncompatibleConfig)
      .setClusterConfig("cats")
      .build()
      .toByteArray,
    "join" -> ClusterMessages.Join
      .newBuilder()
      .setNode(
        ClusterMessages.UniqueAddress
          .newBuilder()
          .setAddress(
            ClusterMessages.Address
              .newBuilder()
              .setHostname("localhost")
              .setPort(1234)
              .setSystem("system1")
              .build()
          )
          .setUid(1)
          .setUid(2)
          .build()
      )
      .addRoles("dc-default")
      .build()
      .toByteArray
  )

  messages.foreach { msg =>
    val bos =
      new BufferedOutputStream(
        new FileOutputStream(s"messages/proto2/${msg._1}.out")
      )
    bos.write(msg._2)
    bos.close()
  }

}
