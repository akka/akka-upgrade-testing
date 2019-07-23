import akka.cluster.protobuf.msg.ClusterMessages

object Main extends App {

  val configCheck = ClusterMessages.ConfigCheck
    .newBuilder()
    .setClusterConfig()

  val uncheckedConfig =
    ClusterMessages.UncheckedConfig
      .newBuilder()
      .build()
      .toByteArray

  ClusterMessages.UncheckedConfig.parseFrom(uncheckedConfig)

  val incompatibleConfig = ClusterMessages.IncompatibleConfig
    .newBuilder()
    .build()

  val compatibleConfig = ClusterMessages.CompatibleConfig
    .newBuilder()
    .setClusterConfig()

}
