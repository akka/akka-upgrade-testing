package io.akka.upgrade

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.util.ManifestInfo
import akka.util.Timeout
import org.slf4j.LoggerFactory

object CollectVersions {

  private val log = LoggerFactory.getLogger("CollectVersions")

  final case class GetVersion(replyTo: ActorRef[Version]) extends CborSerializable
  case class Version(address: String, version: String) extends CborSerializable

  object VersionActor {

    val serviceKey = ServiceKey[GetVersion]("version")

    def apply(): Behavior[GetVersion] = {
      Behaviors.setup { context =>
        context.system.receptionist ! Receptionist.Register(serviceKey, context.self)

        Behaviors.receive { case (context, GetVersion(replyTo)) =>
          replyTo ! Version(
            Cluster(context.system).selfMember.address.toString,
            ManifestInfo(context.system).versions("akka-actor").version)
          Behaviors.same
        }

      }
    }
  }

  def collect(system: ActorSystem[_]): Future[Seq[Version]] = {
    implicit val sys = system
    import akka.actor.typed.scaladsl.AskPattern._
    import system.executionContext
    implicit val timeout: Timeout = 5.seconds

    val allRefs: Future[List[ActorRef[GetVersion]]] = system.receptionist
      .ask[Receptionist.Listing](replyTo => Receptionist.Find(VersionActor.serviceKey, replyTo))
      .map(_.allServiceInstances(VersionActor.serviceKey).toList)

    allRefs.map { refs =>
      log.info("Collecting versions for members: [{}]", refs.map(_.path.address).mkString(", "))
      Future.sequence(refs.map { ref =>
        ref.ask[Version](replyTo => GetVersion(replyTo))
      })
    }.flatten
  }
}
