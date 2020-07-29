package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object VotingSystem extends App{
  type Candidate = String
  type Count = Int
  type CitizenRef = ActorRef
  class Citizen extends Actor{
    import Citizen._
    import VoteAggregator._
    var votedCandidate :Option[Candidate] = None
    override def receive: Receive = {
      case Vote(candidate) ⇒ votedCandidate = Some(candidate)
      case VoteStatusRequest ⇒ sender() ! VoteStatusReply(votedCandidate)
    }

  }

  object Citizen{
    case class Vote(candidate:String)
    case object VoteStatusRequest
  }
  object VoteAggregator{
    case class AggregateVotes(citizens : Set[CitizenRef]) // will sent each of the citizens each of the vote status request
    case class VoteStatusReply(candidate: Option[Candidate])
  }
  class VoteAggregator extends Actor {
    var votes : Map[Candidate,Count] = Map()
    var waitingCitizens: Set[CitizenRef] = Set()
    import VoteAggregator._
    import Citizen._
    override def receive: Receive = {
      case AggregateVotes(citizens) ⇒
        waitingCitizens = citizens
        citizens.foreach(citizen ⇒ citizen ! VoteStatusRequest)
      case VoteStatusReply(None) ⇒
        sender() ! VoteStatusRequest //might end in infinite if not all citizens vote! but this is our protocol
      case VoteStatusReply(Some(candidate)) ⇒
        val newWaitingCitizens = waitingCitizens - sender()
        val newVote: Count = votes.getOrElse(candidate,0) + 1
        votes = votes + (candidate → newVote) //updating vote of candidate
        if(newWaitingCitizens.isEmpty){
          println(s"[VoteAggregator] poll status $votes")
        }else{
          waitingCitizens = newWaitingCitizens
        }
    }
  }

  val system = ActorSystem("VotingSystem")
  val alice = system.actorOf(Props[Citizen],"alice")
  val bob = system.actorOf(Props[Citizen],"bob")
  val charlie = system.actorOf(Props[Citizen],"charlie")
  val daniel = system.actorOf(Props[Citizen],"daniel")
  import  Citizen._
  alice ! Vote("Martin")
  bob ! Vote("Aviad")
  charlie ! Vote("Ronald")
  daniel ! Vote("Ronald")

  val voteAggregator= system.actorOf(Props[VoteAggregator])
  import VoteAggregator._
  voteAggregator ! AggregateVotes(Set(alice,bob,charlie,daniel))
  /*
    Prints status of votes
    Martin -> 1
    Aviad -> 1
    Ronald ->2
   */

}
