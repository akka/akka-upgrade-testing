package io.akka.upgrade

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.RootActorPath
import akka.cluster.Cluster
import akka.util.ManifestInfo
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

object CollectVersions {

  def props() = Props(new VersionActor())

  case object GetVersion
  case class Version(version: String)
  class VersionActor extends Actor {
    override def receive: Receive = {
      case GetVersion =>
        sender() ! Version(
          ManifestInfo(context.system).versions("akka-actor").version
        )
    }
  }

  def collect(system: ActorSystem): Future[Set[Version]] = {
    import system.dispatcher
    import akka.pattern.ask
    implicit val timeout = Timeout(5.second)
    Future
      .traverse(Cluster(system).state.members)(
        member =>
          system
            .actorSelection(RootActorPath(member.address) / "user" / "version")
            .resolveOne(5.second)
      )
      .flatMap(
        (allRefs: Set[ActorRef]) =>
          Future.traverse(allRefs)(ref => ref.ask(GetVersion).mapTo[Version])
      )

  }
}
