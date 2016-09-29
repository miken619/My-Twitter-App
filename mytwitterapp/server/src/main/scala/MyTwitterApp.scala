package MyTwitterApp


import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.events.WebSocketHandshakeEvent
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import akka.dispatch.OnComplete

import akka.actor.{ActorSystem, Props, actorRef2Scala}

import java.util.GregorianCalendar
import java.text.SimpleDateFormat

object MyTwitterApp extends Logger{
  
   val actorSystem = ActorSystem("MyTwitterAppSystem")
   val router = actorSystem.actorOf(Props[Supervisor], name = "router")
   
   val dateFormatter = new SimpleDateFormat("HH:mm:ss")
   val time = new GregorianCalendar()
   val ts = dateFormatter.format(time.getTime())
  
   val routes = Routes({

    case HttpRequest(httpRequest) => httpRequest match {
      
      case GET(Path("/html")) => {
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[MyTwitterHandler]) ! httpRequest
      }
      case Path("/favicon.ico") => {
        // If favicon.ico, just return a 404 because we don't have that file
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
      }
      case Host("localhost:8888")=> {
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[MyTwitterHandler]) ! httpRequest
      }
    }
    
    case WebSocketHandshake(wsHandshake) => wsHandshake match {
        case Path("/websocket/") => {
        // To start Web Socket processing, we first have to authorize the handshake.
        // This is a security measure to make sure that web sockets can only be established at your specified end points.
        wsHandshake.authorize( onComplete = Some(onWebSocketHandshakeComplete),
                               onClose = Some(onWebSocketClose))
      }
    }
     case WebSocketFrame(wsFrame) => {
      // Once handshaking has taken place, we can now process frames sent from the client
      actorSystem.actorOf(Props[MyTwitterHandler]) ! (wsFrame,router)
    }

  })
  
  val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
   
  def main(args: Array[String]) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()
    System.out.println("Open a few browsers and navigate to http://localhost:8888/html. Start twittering!")
  }

  def onWebSocketHandshakeComplete(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId connected")
  }

  def onWebSocketClose(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId closed")
  }

}