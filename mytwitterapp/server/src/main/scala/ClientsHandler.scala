package MyTwitterApp



import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.util.Timeout
import akka.pattern.ask


import scala.collection.mutable.HashMap
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

import java.util.GregorianCalendar
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class ClientsHandler extends Actor{
  
  val log = Logging(context.system, this)
  val dateFormatter = new SimpleDateFormat("HH:mm:ss")
  val time = new GregorianCalendar()
  val ts = dateFormatter.format(time.getTime())
  
  //Map of following of a connected client
 
  var connectedClient = HashMap[String, String]()
  implicit val ec = ExecutionContext.Implicits.global
  implicit lazy val timeout = Timeout(30, TimeUnit.SECONDS)
  
  def receive = {

    case CLIENTCONNECT(cliendID, username,networkHandler) =>
      if(connectedClient.contains(username)) {
          log.info(ts + " - " + username + " already connected") 
      }else {
          log.info(ts + " - " + username + " connection succesful") 
          connectedClient += (cliendID -> username)
      }
    
        val supervisorSender = sender
        val future = networkHandler ? NETWORKCONNECT(username)
        future.onSuccess{
          case CONNECTRESPONSE(response) =>
                log.info(ts + " - NetworkConnect successful")
                val clientResponse = " - " + response + "\nLogin successful for: " + username
                supervisorSender ! CONNECTRESPONSE(clientResponse)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - - NetworkConnect exception")
                val failure = ts + " - NetworkConnect exception"
                supervisorSender ! CONNECTRESPONSE(failure)
          
        }
    case CLIENTDISCONNECT(username)=>
      var response = ""
      if(connectedClient.contains(username)) {
          log.info(ts + " - Logout succesful for: " + connectedClient(username)) 
          response = ts + " - Logout succesful for: " + connectedClient(username)
          connectedClient -= username
      }else {
          log.info(ts + " - " + username + " already logout")    
          response = ts + " - " + username + " already logout"
      }
      sender ! DISCONNECTRESPONSE(response)
      
    case FORWARDFOLLOWERS(username,networkHandler) =>
      val swapMap = connectedClient.map(_.swap)
      if(swapMap.contains(username)) {
          log.info(ts + " getting list to followers") 
      }else {
          log.info(ts + " unable to get list to forward tweets") 
      }
        var followersList = List[String]()
        
        val supervisorSender = sender
        val future = networkHandler ? GETFOLLOWERSLIST(username)
        future.onSuccess{
          case FOLLOWERSLIST(followers) =>           
                log.info(ts + " list obtain")
                for(follower <- followers) {
                   if(swapMap.contains(follower))
                      followersList = swapMap(follower) :: followersList
                }              
                supervisorSender ! FOLLOWERS(followersList)
                  
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " Forwarding failure")
                val temp = ts + " Forwarding failure"
                supervisorSender ! FOLLOWERS(List.empty[String])
          
        }
    case GETUSERNAME(username) =>
      if(connectedClient.contains(username)) {
          log.info(ts + " - Retrieving user name: " + connectedClient(username))
          sender ! USERNAME(connectedClient(username))
      }else {
          log.info(ts + " - Unable to retrieve user name") 
          sender ! USERNAME("")
      }
    
    
  }
  
}