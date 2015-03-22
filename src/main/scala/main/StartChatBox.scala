package main

import actors.ChatBox
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

/**
 * Created by android on 21/3/15.
 */
object StartChatBox {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val chatSystem = ActorSystem("ChatSystem", config)
    val chatBox = chatSystem.actorOf(Props[ChatBox], name = "ChatBox")
  }
}
