# ChatBox
Simple Chat System Using Akka Actors (Akka Remoting)

## ChatBox Actor

Manages the Client Actors and Client Actors register with the ChatBox Actor. The IP of the ChatBox actor is available to Client Actors. 
ChatBox actor watches each Client actor that registers itself with it and removes them from the list of users once Client actors
are unreachable.

_**Conf for ChatBoxActor**_

application.conf

```

    akka {
      loglevel = "INFO"
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        enabled-transports = ["akka.remote.netty.tcp"]
        netty.tcp {
          hostname = "127.0.0.1"
          port = 2222
        }
        log-sent-messages = on
        log-received-messages = on
      }
    }
    
```

_**ChatBox.scala**_

```scala

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
    
```


## Client Actor

Each Client Actor represents the User and registers with the ChatBox Actor initially. Registration
happens by first looking up the actor using `context.actorSelection("akka.tcp://ChatSystem@someip:port/user/ChatBox")`
and then the Register message is sent by the client to the ChatBox actor thus registering with the chatbox actor.

_**Conf for ClientActor**_

client.conf

```

    akka {
      loglevel = "INFO"
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        enabled-transports = ["akka.remote.netty.tcp"]
        netty.tcp {
          hostname = "127.0.0.1"
          port = 0
        }
        log-sent-messages = on
        log-received-messages = on
      }
    }
    
```

_**Client.scala**_

```scala
    
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
        chatBox = Some(context.actorSelection("akka.tcp://ChatSystem@$ip:2222/" +
          "user/ChatBox")) // node that chat box actor lookup is done using ChatBoxActor Running machine IP.
          //localhost if both ChatBoxActor and Client Actor are running on same machine.
    
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


```

## Main

_**Start ChatBox first on a Machine and use the IP of the ChatBox to lookup ChatBox from Client**_

_**StartChatBox**_

```scala

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

```

_**StartClient**_

```scala
    
    package main

    import actors.Client
    import akka.actor.{Props, ActorSystem}
    import com.typesafe.config.ConfigFactory

    /**
     * Created by android on 21/3/15.
     */
    object StartClient {
      def main(args: Array[String]): Unit = {
        if (args.isEmpty) {
          println("Please provide IP of ChatBox Actor as the commandline argument")
          System.exit(0)
        }
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
    
```


## Usage

Start ChatBox Actor

` sbt "runMain main.StartChatBox" //note the IP of the machine `


now Start Client Actor

` sbt "runMain main.StartClient 127.0.0.1" // IP of the machine running chatbox actor `