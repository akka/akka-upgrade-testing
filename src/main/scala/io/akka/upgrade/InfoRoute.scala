package io.akka.upgrade

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object InfoRoute {
  case class Info(akkaVersion: String)
}

class InfoRoute(system: ActorSystem[_]) extends SprayJsonSupport {
  val Log = LoggerFactory.getLogger(classOf[InfoRoute])
  implicit val itemFormat: RootJsonFormat[CollectVersions.Version] =
    jsonFormat2(CollectVersions.Version)

  // Retry once before failing (and errors being logged which fails the test).
  // The receptionist may have not removed a node that is leaving so one of the
  // version asks may timeout
  val route: Route = get {
    path("versions") {
      import akka.pattern.retry
      import scala.concurrent.duration._
      extractActorSystem { as =>
        complete(retry(() => CollectVersions.collect(system), 2, 500.millis)(as.dispatcher, as.scheduler))
      }
    }
  }
}
