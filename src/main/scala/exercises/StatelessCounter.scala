package exercises

import akka.actor.{Actor, ActorSystem, Props}

object StatelessCounter extends App{
  val system = ActorSystem("CounterSystem")
  case object Increment
  case object Decrement
  case object Print
  class Counter extends Actor{
    override def receive: Receive = countReceive(0)

    def countReceive(currentCount:Int) : Receive = {
      case Increment ⇒
        println(s"incrementing [$currentCount]")
        context.become(countReceive(currentCount+1),discardOld = true)
      case Decrement ⇒
        println(s"decrementing [$currentCount]")
        context.become(countReceive(currentCount-1),discardOld = true)
      case Print ⇒ println(currentCount)
    }
  }
  val counter = system.actorOf(Props[Counter])
  (1 to 3).foreach(_ ⇒ counter ! Increment)
  (1 to 5).foreach(_ ⇒ counter ! Decrement)
  counter ! Print
}
