package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}




class BankAccountActor extends Actor {
  import BankAccountActor._
  var money:Int = 0
  override def receive: Receive = {
    case Deposit(x) ⇒
      if(x < 0) sender() ! TransactionFailure("invalid deposit amount")
      else {
        money += x
        sender() ! TransactionSuccess(s"Deposited $x in account [${self.path}]")
      }
    case Withdraw(x) ⇒
      if(x<0) sender() ! TransactionFailure("invalid withdraw amount")
      else if(x>money) sender() ! TransactionFailure(s"you don't have enough money to withdraw that $x")
      else {
        money -= x
        sender() ! TransactionSuccess(s"withdrew $x from your account, currently you have $money")
      }
    case Statement ⇒ sender() ! s"Your current balance is $money" //replying to sender
    case message ⇒ println(message)
  }
}
//companion
object BankAccountActor {

  trait BankActions

  case class Deposit(amount: Int) extends BankActions

  case class Withdraw(amount: Int) extends BankActions

  case object Statement extends BankActions

  case class TransactionSuccess(message:String) extends BankActions

  case class TransactionFailure(message:String) extends BankActions

}
object Person{
  case class UseAccountToLiveLikeARich(bankAccount: ActorRef)
}
class Person extends Actor {
  import Person._
  import BankAccountActor._
  override def receive: Receive = {
    case UseAccountToLiveLikeARich(bankAccount) ⇒
      bankAccount ! Deposit(1_000_000)
      bankAccount ! Withdraw(20_000)
      bankAccount ! Withdraw(2_000_000)
      bankAccount ! Withdraw(500_000)
      bankAccount ! Statement
    case responseFromBank ⇒ println(responseFromBank)
  }
}

object TestBankAccount extends App {

  val system = ActorSystem("Bank")
  import Person._
  val account = system.actorOf(Props[BankAccountActor],"bankAccountOfBillioner")
  val person = system.actorOf(Props[Person],"billioner")

  person ! UseAccountToLiveLikeARich(account)


}