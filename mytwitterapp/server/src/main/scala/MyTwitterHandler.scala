package MyTwitterApp



import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.WebSocketFrameEvent

import scala.io.Source

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import akka.event.Logging

import java.util.GregorianCalendar
import java.text.SimpleDateFormat
 


class MyTwitterHandler extends Actor{
  
  val log = Logging(context.system, this)
  val dateFormatter = new SimpleDateFormat("HH:mm:ss")
  val time = new GregorianCalendar()
  val ts = dateFormatter.format(time.getTime())
  
  
  def receive = {
    case event: HttpRequestEvent =>
      // Return the HTML page to setup web sockets in the browser
      writeHTML(event)
      context.stop(self)
    case (event: WebSocketFrameEvent, response: String)=>
      // Echo web socket text frames
      writeWebSocketResponse(event,response)
      context.stop(self)  
    case (event: WebSocketFrameEvent, router: ActorRef)=>
       // Process web socket text frames
      processWebSocketFrame(event, router)
      context.stop(self)
    case TWEET(username: String,tweet: String) =>
      // Echo web socket text frames
      writeWebSocketTweets(username,tweet)
    case _ => {
      log.info("received unknown message of type: ")
      context.stop(self)
    }
  }
  
  
  /**
   * Write HTML page to setup a web socket on the browser
   */
  private def writeHTML(ctx: HttpRequestEvent) {
    // Send 100 continue if required
    if (ctx.request.is100ContinueExpected) {
      ctx.response.write100Continue()
    }
    
     val buf = new StringBuilder()
     for(line <- Source.fromURL(getClass.getResource("/UI.html")).getLines) {
        buf.append(line)
     }

    ctx.response.write(buf.toString, "text/html; charset=UTF-8")
  }
    
    
  private def writeWebSocketResponse(event: WebSocketFrameEvent, response: String) {
    log.info(ts + " Response " + response)
    MyTwitterApp.webServer.webSocketConnections.writeText(ts + " " + response,event.webSocketId)
  }
  
  private def writeWebSocketTweets(username: String, tweets: String) {
    log.info(ts + " Tweets " + tweets)
    MyTwitterApp.webServer.webSocketConnections.writeText(ts + " " + tweets, username)
  }
  
  private def processWebSocketFrame(event: WebSocketFrameEvent, router: ActorRef){
      log.info(ts + " Processing Text Frame for " + event.webSocketId)
      
      if(event.readText.charAt(0) == '0') {
           if(event.readText.contains("*@")) {
               log.info(ts + " Attempting to login")
               router ! LOGIN(event)
           }else if(event.readText.contains("@")) {
               log.info(ts + " Attempting to logout")  
               router ! LOGOUT(event)
           }else if(event.readText.contains("#")) {
               log.info(ts + " Attempting to tweet tag message" )  
               router ! TWEETTAGMESSAGE(event)
           }else {
               log.info(ts + " Attempting to tweet message" )  
               router ! TWEETMESSAGE(event)  
           }      
      }else if(event.readText.charAt(0) == '1') {         
           if(event.readText.contains("*@")) {
               log.info(ts + " Attempting to get followers")   
               router ! GETFOLLOWERS(event)
           }else if(event.readText.contains("$@")) {
                log.info(ts + " Attempting to get followering")   
                router ! GETFOLLOWINGS(event)
           }else {
                 log.info(ts + " Error processing Text Frame " + event.readText)
                 var temp = "Error processing Text Frame"
                 writeWebSocketResponse(event,temp)
           }         
      }else if (event.readText.charAt(0) == '2') {
           if(event.readText.contains("@")) {
               log.info(ts + " Getting messages")  
               router ! GETUSERMESSAGES(event)
           }else if(event.readText.contains("#")) {
               log.info(ts + " Getting tag messages")   
               router ! GETTAGMESSAGES(event)
           }else {
               log.info(ts + " Searching failes")   
           }
      }else if (event.readText.charAt(0) == '3') {
           if(event.readText.contains("*@")) {
                log.info(ts + " Attemping to follow") 
                router ! STARTFOLLOWING(event)
           }else if(event.readText.contains("$@")) {
                log.info(ts + " Attemping to stop follow")  
                router ! STOPFOLLOWING(event)
           }else {
                log.info(ts + " Error processing Text Frame")
                var temp = "Error processing Text Frame "
                writeWebSocketResponse(event,temp)
           } 
      }else {
          log.info(ts + " Error processing Text Frame")
          var temp = "Error processing Text Frame"
          writeWebSocketResponse(event,temp)
      }
     
   }
  
}