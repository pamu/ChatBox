package main

import actors.Client
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

/**
 * Created by android on 21/3/15.
 */
object StartClient {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load("client")
    val clientSystem = ActorSystem("ClientSystem", config)
    println("Enter your Nick Name:")
    var name = Console.readLine.trim
    while (name == "") {
      println("Enter your Nick Name:")
      name = Console.readLine.trim
    }
    val client = clientSystem.actorOf(Props(new Client(name)), "Client")

    println("Type message end with -> after -> type name of the person to send " +
      "and hit enter to send messages")

    while (true) {
      val line = Console.readLine.trim
      if (line != "" && line.contains("->") && line.split("->").size == 2) {
        val texts = line.split("->")
        client ! Client.SendMessage(texts(1).trim, texts(0).trim)
      }
    }
  }
}
