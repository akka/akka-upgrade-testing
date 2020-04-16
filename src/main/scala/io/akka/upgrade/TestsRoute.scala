package io.akka.upgrade

import scala.concurrent.duration._

import akka.actor.typed.ActorSystem
import akka.cluster.ddata.GSet
import akka.cluster.ddata.GSetKey
import akka.cluster.ddata.typed.scaladsl.DistributedData
import akka.cluster.ddata.typed.scaladsl.Replicator
import akka.cluster.ddata.typed.scaladsl.Replicator.UpdateResponse
import akka.cluster.typed.Cluster
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.akka.upgrade.InfoRoute._
import spray.json.DefaultJsonProtocol._

object TestsRoute {

  final case class Data(s: String) extends CborSerializable

  val ddataKey = GSetKey[Data]("data")

}

class TestsRoute(system: ActorSystem[_]) extends SprayJsonSupport {
  import TestsRoute._

  implicit val itemFormat = jsonFormat1(Info)

  implicit val selfNode = DistributedData(system).selfUniqueAddress

  val route: Route = get {
    pathPrefix("test") {
      path("sharding") {
        complete("TODO sharding")
      } ~
      path("ddata") {
        implicit val sys = system
        import akka.actor.typed.scaladsl.AskPattern._
        import system.executionContext
        implicit val timeout: Timeout = 5.seconds

        val item = Data(Cluster(system).selfMember.address.toString)

        val response = DistributedData(system).replicator.ask[UpdateResponse[GSet[Data]]](replyTo =>
          Replicator.Update(ddataKey, GSet.empty[Data], Replicator.WriteMajority(timeout.duration), replyTo)(_ + item))

        val result = response.map(_ match {
          case _: Replicator.UpdateSuccess[_] => "OK"
          case _                              => response.toString
        })

        complete(result)
      }
    }
  }
}
