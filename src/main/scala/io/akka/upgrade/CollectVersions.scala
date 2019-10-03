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
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

object CollectVersions {

  private val Log = LoggerFactory.getLogger("CollectVersions")

  def props() = Props(new VersionActor())

  case object GetVersion
  case class Version(address: String, version: String)
  class VersionActor extends Actor {
    override def receive: Receive = {
      case GetVersion =>
        sender() ! Version(
          Cluster(context.system).selfMember.address.toString,
          ManifestInfo(context.system).versions("akka-actor").version)
    }
  }

  def collect(system: ActorSystem): Future[Seq[Version]] = {
    import system.dispatcher
    import akka.pattern.ask
    implicit val timeout = Timeout(5.second)
    val members = Cluster(system).state.members
    Log.info("Collecting versions for members: {}", members)
    Future
      .traverse(members.toSeq)(member =>
        system.actorSelection(RootActorPath(member.address) / "user" / "version").resolveOne(5.second))
      .flatMap((allRefs: Seq[ActorRef]) => Future.traverse(allRefs)(ref => ref.ask(GetVersion).mapTo[Version]))

  }
}
