package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.immutable.Queue

object ChildActors extends App {

  object WordCounterMaster{
    case class Initialize(nChildren:Int)
    case class WordCountTask(id:Int,text: String)
    case class WordCountReply(id:Int,count:Int)
  }
  //Distributed word counting
  class WordCounterMaster extends Actor {
    import WordCounterMaster._
    var queue :Queue[ActorRef] = Queue()
    override def receive: Receive = {
      case Initialize(n) ⇒ {
        println("[master] initializing...")
        val workers: Seq[ActorRef] = for(i <- 1 to n) yield context.actorOf(Props[WordCounterWorker],s"worker$i")
        context.become(withChildren(workers,0,0,Map()))
      }
    }

    def withChildren(workers: Seq[ActorRef], i:Int,currentTaskId: Int,requestMap:Map[Int,ActorRef]): Receive = {
      case text:String =>
        println(s"[master] I recived a $text, I will send it to worker$i")
        val originalSender = sender()
        val childRef = workers(i)
        val task = WordCountTask(currentTaskId,text)
        childRef ! task
        val nextChildIndex = (i+1) % workers.length
        val newTaskId= currentTaskId+1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(workers,nextChildIndex,newTaskId,newRequestMap))

      case WordCountReply(id,count) =>
        println(s"[master] reply with task id $id, count $count")
        requestMap(id) ! count
        //message handled so we can remove it from requestMap
        context.become(withChildren(workers, i, currentTaskId, requestMap-id))
    }

  }

  class WordCounterWorker extends Actor{
    import WordCounterMaster._

    def wordCount(text: String): Int = text.split(" ").length

    override def receive: Receive = {
      case WordCountTask(id,text) ⇒
        println(s"${self.path}, I have received a $text , task $id")
        sender() ! WordCountReply(id,wordCount(text))
    }
  }

  class TestWordCount extends Actor{
    override def receive: Receive = {
      case "go" =>
      val master = context.actorOf(Props[WordCounterMaster],"master")
      val texts = List("I love scala","Akka is fine too","Yay!","Aviad shiber rules ! !")
      import WordCounterMaster._
        master ! Initialize(3)
        texts.foreach(txt => master ! txt)
      case count:Int => println(s"I received a reply with count $count")

    }
  }
  /*
      create WordCounterMaster
      send Initialize(10) to wordCounterMaster
      send "Akka is awesome" to wordCounterMaster
        wcm will send a WordCountTask("...") to one of its children
          child replies with a WordCountReply(3) to the master
        master replies with 3 to the sender.
      requester -> wcm -> wcw
             r  <- wcm <-
     */
  // round robin logic
  val system = ActorSystem("distWordCount")
  val test = system.actorOf(Props[TestWordCount],"testingWordCount")
  test ! "go"


}
