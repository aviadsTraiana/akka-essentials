package exercises.infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

object TimersAndSchedulers extends App{
  private class SimpleActor extends Actor with ActorLogging{
      override def receive: Receive = {
          case message => log.info("message:"+message.toString)
      }
  }

  val system = ActorSystem("SchedulersAndTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling a reminder for simpleActor")
  import system.dispatcher
 /* system.scheduler.scheduleOnce(1 second){
    simpleActor ! "reminder"
  }
  val routine: Cancellable = system.scheduler.schedule(initialDelay = 1 second, interval = 2 seconds){
    simpleActor ! "heartBeat"
  }

  system.scheduler.scheduleOnce(5 seconds){
    routine.cancel()
  }*/
  /**
   * Exercise: implement a self-closing actor
   *
   * - if the actor receives a message (anything), you have 1 second to send it another message
   * - if the time window expires, the actor will stop itself
   * - if you send another message, the time window is reset
   */
  private class SelfClosingActor extends Actor with ActorLogging{
    var schedule: Cancellable = createTimeoutWindow()
    def createTimeoutWindow(): Cancellable ={
      context.system.scheduler.scheduleOnce(1 second){
        self ! "timeout"
      }
    }
    override def receive: Receive = {
      case "timeout" => context.stop(self)
      case message =>
        log.info(s"$message")
        schedule.cancel()
        schedule= createTimeoutWindow()

    }
  }

  /**
   * Or we can use Timers - which meant to deal with sending scheduled messages to self
   */

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.warning("Stopping!")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerHeartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor], "timerActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerHeartbeatActor ! Stop
  }



}
