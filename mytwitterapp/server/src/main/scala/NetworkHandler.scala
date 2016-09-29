package MyTwitterApp



import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

import scala.collection.mutable.HashMap
import scala.collection.mutable.Set

import java.util.GregorianCalendar
import java.text.SimpleDateFormat


class NetworkHandler extends Actor{
  
  val log = Logging(context.system, this)
  val dateFormatter = new SimpleDateFormat("HH:mm:ss")
  val time = new GregorianCalendar()
  val ts = dateFormatter.format(time.getTime())
  
  
  //List of following of a user
  var users = HashMap[String, Set[String]]()
  
  //List of followers of a user
  var followers = HashMap[String, Set[String]]()
  
  
  
  def receive = {
    case NETWORKCONNECT(username) =>
      var response = ""
      if(users.contains(username)) {
          log.info(ts + " " + username + "  already registered")
          response = "Already registered"
      }else {
          log.info(ts + " Registering new user: " + username)
          users += (username -> Set[String]())
          followers += (username -> Set[String]())     
          response = "Registering new user" 
      }
      
      if(!followers.contains(username)) {
          followers += (username -> Set[String]())     
      }
      
      sender ! CONNECTRESPONSE(response)
      
    case GETFOLLOWINGSLIST(username) =>
      
         
         if(users.contains(username)) { 
           log.info(ts + " - Returning following's list")
           sender ! FOLLOWERSLIST(users(username))
        }else {
           log.info(ts + " - Unable to return following's list")
           sender ! FOLLOWERSLIST(Set[String]())       
        }
         
         
          
     case GETFOLLOWERSLIST(username) =>
        
        if(followers.contains(username)) { 
            log.info(ts + " - Returning followers's list")
            sender ! FOLLOWERSLIST(followers(username))
        }else {
           log.info(ts + " - Unable to return followers's list")
           sender ! FOLLOWERSLIST(Set[String]())       
        }
      
   
     case FOLLOWS(username,following) =>
            log.info(ts + " - Attempting to follow: " + following) 
            var response = ""
            if(users.contains(username)) {
                  users += (username -> (users(username) += following))
                  response = "You're now following: " + following
            }else {
                 response = "Cannot follow: " + following
            }
           
           if(followers.contains(following)) { 
                 followers += (following -> (followers(following) += username))
           }
           
           
           sender ! FOLLOWINGS(response)       
             
        
     case STOPFOLLOWS(username,following) =>
      
            log.info(ts + " - Attempting to unfollow: " + following)
            var response = ""
            if(users.contains(username)) {
                  users += (username -> (users(username) -= following))
                  response = "You unfollow: " + following
            }else {
                 response = "Cannot unfollow: " + following
            }
           
           if(followers.contains(following)) { 
                 followers += (following -> (followers(following) -= username))
           }
           
           sender ! FOLLOWINGS(response)       
           
        
  }
}