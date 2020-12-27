package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object VotingSystem extends App{
  type Candidate = String
  type Count = Int
  type CitizenRef = ActorRef
  class Citizen extends Actor{
    import Citizen._
    import VoteAggregator._
    //var candidate: Option[Candidate] = None
    override def receive: Receive = {
      case Vote(candidate) ⇒ context.become(votedTo(candidate)) //this.candidate = candidate
      case VoteStatusRequest ⇒ sender() ! VoteStatusReply(None) //in case we got request but never vote before
    }
    def votedTo(candidate: Candidate) : Receive = {
      case VoteStatusRequest ⇒ sender() ! VoteStatusReply(Some(candidate))
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
//    var votes : Map[Candidate,Count] = Map()
//    var waitingCitizens: Set[CitizenRef] = Set()
    import VoteAggregator._
    import Citizen._
    override def receive: Receive = initReceiver

    def initReceiver: Receive ={
      case AggregateVotes(citizens) ⇒ {
        citizens.foreach(citizen ⇒ citizen ! VoteStatusRequest)
        context.become(awaitPollStatus(citizens,Map()))
      }
    }
    def awaitPollStatus(citizens: Set[CitizenRef], votes: Map[Candidate, Count]): Receive ={
      case VoteStatusReply(None) ⇒
        sender() ! VoteStatusRequest //might end in infinite if not all citizens vote! but this is our protocol
      case VoteStatusReply(Some(candidate)) ⇒
        val newWaitingCitizens = citizens - sender()
        val newVote: Count = votes.getOrElse(candidate,0) + 1
        val newVotes = votes + (candidate→ newVote) //updating vote of candidate
        if(newWaitingCitizens.isEmpty){
          println(s"[VoteAggregator] poll status $newVotes")
        }else{
          context.become( awaitPollStatus(newWaitingCitizens,newVotes))
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
