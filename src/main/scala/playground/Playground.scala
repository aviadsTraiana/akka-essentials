package playground

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Playground extends App{

  val actorSystem= ActorSystem("WordCountSystem")
  println(actorSystem.name)

  // create actors
  class WordCountActor extends Actor{
    var totalWords = 0 //encapsulated data

    //behaviour
    def receive: Receive =  { //Receive is alias to PartialFunction[Any, Unit]
      case message : String ⇒ {
        println(s"[word count] I have received a message [$message]")
        totalWords += message.split(" ").length
      }
      case otherMessageType ⇒ println(s"[word count] I cannot understand this message: [$otherMessageType]")
    }
  }


  //instantiate actor
  val wordCounter: ActorRef = actorSystem.actorOf(Props[WordCountActor],"wordCounter")
  val anotherActor: ActorRef = actorSystem.actorOf(Props[WordCountActor],"anotherWordCounter")

  //send an async message call to actor
  wordCounter ! "I am learning Akka :)"
  anotherActor ! "another message"

  case class SpecialMessage(content: String)
  //instantiate actor with constructor arguments
  class Person(name:String) extends Actor {
    //context.self == self == this in oop
    override def receive: Receive = {
      case "Hi!" ⇒ sender ! "Hi to you too" // replying to sender
      case message: String ⇒ println(s"[$self] my name is $name and I got your message [$message]")
      case number : Int ⇒ println(s"[${context.self}] got a number" + number)
      case SpecialMessage(content) ⇒ self ! content //tell to myself the content in async manner to message(content)
      case SayHiTo(otherActor) ⇒ otherActor ! "Hi!"
      case WirelessPhoneMessage(content,ref) ⇒ ref forward (content+"s") //save the original sender (noSender)
      case _ ⇒ println(s"[${context.self}]I have no idea what you want from me dude")
    }
  }
  //discouraged
  val personActor= actorSystem.actorOf(Props(new Person("Aviad")),"personActor")
  personActor ! "please call me"
  //better practice - declare companion object that returns Props objects (factory method)
  object Person{
    def props(name:String): Props = Props(new Person(name))
  }
  val aviadActor = actorSystem.actorOf(Person.props("Aviad"),"aviadActor")
  //messages can be of any type, but IMMUTABLE and serializable! (this why we use case class/object)
  aviadActor ! "please call me"
  aviadActor ! 5
  aviadActor ! SpecialMessage("some content")


  //actors can reply to messages
  val alice = actorSystem.actorOf(Person.props("alice"),"aliceActor")
  val bob = actorSystem.actorOf(Person.props("bob"),"bobActor")
  case class SayHiTo(ref:ActorRef)

  alice ! SayHiTo(bob)
  // send to dlq (since sender is null and hi handler try to send to null)
  alice ! "Hi!"

  //forward messages (D -> A -> B) = sending a message with the original sender
  case class WirelessPhoneMessage(content: String ,ref: ActorRef)

  alice ! WirelessPhoneMessage("Hi again!!",bob) //noSender is the original sender

}
