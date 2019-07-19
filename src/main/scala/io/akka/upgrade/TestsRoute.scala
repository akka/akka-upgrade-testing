package io.akka.upgrade

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.ManifestInfo
import io.akka.upgrade.InfoRoute._
import spray.json.DefaultJsonProtocol._

class TestsRoute(system: ActorSystem) extends SprayJsonSupport {
  implicit val itemFormat = jsonFormat1(Info)

  val route: Route = get {
    pathPrefix("/test") {
      path("sharding") {
        complete("TODO sharding")
      } ~
        path("ddata") {
          complete("TODO ddata")
        }
    }
  }
}
