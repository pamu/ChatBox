package actors

import akka.actor.{ActorLogging, ActorSelection, Actor}

/**
 * Created by android on 21/3/15.
 */
object Client {
  case class SendMessage(to: String, message: String)
  case class ReceiveMessage(from: String, message: String)
}

class Client(name: String, ip: String) extends Actor with ActorLogging {

  import ChatBox._
  import Client._

  var chatBox: Option[ActorSelection] = None

  override def preStart(): Unit = {
    chatBox = Some(context.actorSelection(s"akka.tcp://ChatSystem@$ip:2222/" +
      "user/ChatBox"))

    chatBox.map(actor => actor ! Register(name))

    chatBox.getOrElse({
      println("ChatBox unreachable, shutting down :(")
      context.stop(self)
    })
  }

  override def receive = {
    case SendMessage(to, message) => chatBox.map(actor => actor ! Message(name, to, message))
    case ReceiveMessage(from, message) =>
      println(s"$from says: $message")
    case _ => log.info("unknown message")
  }
}
