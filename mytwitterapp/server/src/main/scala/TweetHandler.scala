package MyTwitterApp

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

import scala.collection.mutable.HashMap
import scala.collection.mutable.Set

import java.util.GregorianCalendar
import java.text.SimpleDateFormat

class TweetHandler extends Actor{

  val log = Logging(context.system, this)
  val dateFormatter = new SimpleDateFormat("HH:mm:ss")
  val time = new GregorianCalendar()
  val ts = dateFormatter.format(time.getTime())
  
  var userTweets = HashMap[String, List[String]]()
  var tagTweets = HashMap[String, List[String]]()
  
  def receive = {
    case GETUSERTWEETS(username) =>
      if(userTweets.contains(username)) {
          log.info(ts + " Retrieving tweets from user")
          sender ! TWEETS(userTweets(username))
      }else {
          log.info(ts + " unable to Retrieve tweets from user") 
          sender ! TWEETS(List[String]())
      }
      
    case GETTAGTWEETS(tag) =>
      if(tagTweets.contains(tag)) {
          log.info(ts + " Retrieving tweets from tag")
          sender ! TWEETS(tagTweets(tag))
      }else {
          log.info(ts + " unable to Retrieve tweets from tag") 
          sender ! TWEETS(List[String]())
      }
    case ADDUSERTWEETS(username,tweets) =>
      if(userTweets.contains(username)) {
          userTweets += (username -> (tweets :: userTweets(username)))  
      }else {
           userTweets += (username -> (tweets :: List[String]()))        
      }
      log.info(ts + " - Tweet added for: " + username)
      var response = "Tweet added for: " + username
      sender ! TWEETRESPONSE(response)
    case ADDTAGTWEETS(tag,tweets) =>
      if(tagTweets.contains(tag)) {
          tagTweets += (tag -> (tweets :: tagTweets(tag)))
      }else {
           tagTweets += (tag -> (tweets :: List[String]()))
      }
      log.info(ts + " - Tweet added for: " + tag)
      var response = "Tweet added for: " + tag
      sender ! TWEETRESPONSE(response)      
  }
  
}