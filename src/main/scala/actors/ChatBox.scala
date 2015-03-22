package actors

import akka.actor.{Terminated, ActorRef, ActorLogging, Actor}

/**
 * Created by android on 21/3/15.
 */

object ChatBox {
  case class Register(name: String)
  case class Message(from: String, to: String, body: String)
}

class ChatBox extends Actor with ActorLogging {

  var clients = Map.empty[String, ActorRef]

  import ChatBox._
  import Client._

  override def receive = {
    case Register(name) => {
     if (! (clients contains name) ) {
       clients += (name -> sender)
       context watch sender
       log.info("Clients")
       log.info(clients.mkString("\n"))
     }
    }

    case Message(from, to, body) =>
      log.info(s"from: $from says: $body to: $to")
      if(clients contains to) {
        clients(to) ! ReceiveMessage(from, body)
      }else {
        log.info("message dropped")
      }

    case Terminated(actor) => {
      clients = clients.filter(_._2 != actor)
    }
    case _ => log.info("unknown message")
  }
}
