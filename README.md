# ChatBox
Simple Chat System Using Akka Actors (Akka Remoting)

## ChatBox Actor

Manages the Client Actors and Client Actors register with the ChatBox Actor. The IP of the ChatBox actor is available to Client Actors. 
ChatBox actor watches each Client actor that registers itself with it and removes them from the list of users once Client actors
are unreachable.

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


