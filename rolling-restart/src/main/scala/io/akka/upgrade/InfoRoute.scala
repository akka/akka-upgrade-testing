package io.akka.upgrade

import akka.AkkaVersion
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import io.akka.upgrade.InfoRoute._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.util.ManifestInfo
import spray.json.RootJsonFormat

object InfoRoute {
  case class Info(akkaVersion: String)
}

class InfoRoute(system: ActorSystem) extends SprayJsonSupport {
  implicit val itemFormat: RootJsonFormat[CollectVersions.Version] =
    jsonFormat1(CollectVersions.Version)

  val route: Route = get {
    path("versions") {
      complete(CollectVersions.collect(system))
    }
  }
}
