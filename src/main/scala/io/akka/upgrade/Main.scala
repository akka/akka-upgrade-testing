package io.akka.upgrade

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.Materializer
import akka.stream.SystemMaterializer

object Main extends App {

  implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Guardian(), "Upgrade")
  implicit val materializer: Materializer = SystemMaterializer(system).materializer

  // FIXME why is this needed? Otherwise compiler complains about routes in bindAndHandle
  implicit val classicSystem = system.toClassic

  val infoRoute = new InfoRoute(system)
  val testsRoute = new TestsRoute(system)
  val clusterRoute = new ClusterRoute(system)
  val routes = concat(infoRoute.route, testsRoute.route, clusterRoute.route)

  val interface =
    if (system.settings.config.getString("akka.remote.artery.canonical.hostname") == "<getHostAddress>")
      "0.0.0.0"
    else
      system.settings.config.getString("akka.remote.artery.canonical.hostname")
  Http(system).bindAndHandle(routes, interface, 8080)
}

object Guardian {
  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val management = AkkaManagement(context.system)
      management.start()
      val bootstrap = ClusterBootstrap(context.system)
      bootstrap.start()

      context.spawn(CollectVersions.VersionActor(), "version")

      Behaviors.empty
    }
  }
}
