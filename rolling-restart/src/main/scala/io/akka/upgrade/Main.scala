package io.akka.upgrade

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer

object Main extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  system.actorOf(CollectVersions.props(), "version")
  val cluster = Cluster(system)
  val management = AkkaManagement(system)
  management.start()
  val bootstrap = ClusterBootstrap(system)
  bootstrap.start()

  val infoRoute = new InfoRoute(system)
  val interface =
    if (system.settings.config.getString(
          "akka.remote.artery.canonical.hostname"
        ) == "<getHostAddress>")
      "0.0.0.0"
    else
      system.settings.config.getString("akka.remote.artery.canonical.hostname")
  Http(system).bindAndHandle(infoRoute.route, interface, 8080)
}
