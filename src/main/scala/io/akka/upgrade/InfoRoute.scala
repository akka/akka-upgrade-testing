package io.akka.upgrade

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object InfoRoute {
  case class Info(akkaVersion: String)
}

class InfoRoute(system: ActorSystem) extends SprayJsonSupport {
  val Log = LoggerFactory.getLogger(classOf[InfoRoute])
  implicit val itemFormat: RootJsonFormat[CollectVersions.Version] =
    jsonFormat2(CollectVersions.Version)

  val route: Route = get {
    path("versions") {
      complete(CollectVersions.collect(system))
    }
  }
}
