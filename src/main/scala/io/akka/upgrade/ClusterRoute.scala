package io.akka.upgrade

import java.lang.reflect.Method

import akka.actor.typed.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.util.control.NonFatal

class ClusterRoute(system: ActorSystem[_]) extends SprayJsonSupport {

  val route: Route = get {
    pathPrefix("cluster") {
      path("prepare-for-shutdown") {
        get {
          // this is compiled with older versions of akka without this method
          val cluster = Cluster(system)
          try {
            val m: Method = cluster.getClass.getMethod("prepareForFullClusterShutdown")
            m.invoke(cluster)
            complete(StatusCodes.OK)
          } catch {
            case NonFatal(t) =>
              complete(StatusCodes.InternalServerError, t.getMessage)
          }
        }
      }
    }
  }
}
