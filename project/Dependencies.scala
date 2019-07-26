import sbt._

object Dependencies {
  val akkaVersion = System.getProperty("override.akka.version", "2.5.22")
  val akkaManagementVersion = "1.0.1"
  val akkaHttpVersion = "10.1.9"

  val akkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion
  val akkaDiscovery = "com.typesafe.akka" %% "akka-discovery" % akkaVersion
  val akkaDiscoveryKubernetes = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion
  val akkaManagementCluster = "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion
  val akkaManagementBootstrap = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion

  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  val serviceDeps =
    Seq(
      akkaClusterSharding,
      akkaDiscovery,
      akkaDiscovery,
      akkaDiscoveryKubernetes,
      akkaManagementCluster,
      akkaManagementBootstrap,
      akkaHttp,
      akkaHttpSprayJson,
      scalaTest % "it"
    )

}
