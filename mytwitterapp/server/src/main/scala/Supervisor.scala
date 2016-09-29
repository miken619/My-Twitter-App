package MyTwitterApp


import akka.actor.{Actor, OneForOneStrategy,Props, Terminated, ActorRef}
import akka.event.Logging
import akka.actor.SupervisorStrategy.Restart
import akka.util.Timeout
import akka.pattern.ask


import org.mashupbots.socko.events.WebSocketFrameEvent

import java.util.GregorianCalendar
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._


class Supervisor extends Actor{

  val log = Logging(context.system, this)
  val dateFormatter = new SimpleDateFormat("HH:mm:ss")
  val time = new GregorianCalendar()
  val ts = dateFormatter.format(time.getTime())
  var user = ""
  implicit val ec = ExecutionContext.Implicits.global
  implicit lazy val timeout = Timeout(15, TimeUnit.SECONDS)
  
  
  override val supervisorStrategy = OneForOneStrategy(){
    case _: Exception => Restart
  }
  
  val clientHandler = context.actorOf(Props[ClientsHandler], name = "clientHandler")
  val tweetHandler = context.actorOf(Props[TweetHandler], name = "tweetHandler")
  val networkHandler = context.actorOf(Props[NetworkHandler], name = "networkHandler")
  
  context.watch(clientHandler)
  context.watch(tweetHandler)
  context.watch(networkHandler)
  
  def receive = {
    case Terminated(`clientHandler`) =>
        log.info(ts + " clientHandler actor terminated.")
    case Terminated(`tweetHandler`) =>
        log.info(ts + " tweetHandler actor terminated.")
    case Terminated(`networkHandler`) =>
       log.info(ts + " networkHandler actor terminated.")
    case LOGIN(event) =>
        val username = event.readText.substring(event.readText.indexOf("@"), event.readText.length)
        val future = clientHandler ? CLIENTCONNECT(event.webSocketId,username,networkHandler)
        future.onSuccess{
          case CONNECTRESPONSE(response) =>
                log.info(ts + " - Login successful for: " + username)
                context.actorOf(Props[MyTwitterHandler]) ! (event,response)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - Login exception for: " + username)
                val failure = ts + " - Login exception for: " + username
                context.actorOf(Props[MyTwitterHandler]) ! (event,failure) 
        }
    case LOGOUT(event) =>
        val username = event.readText.substring(event.readText.indexOf("@"), event.readText.length)
        val future = clientHandler ? CLIENTDISCONNECT(event.webSocketId)
        future.onSuccess{
          case DISCONNECTRESPONSE(response) =>
                log.info(response)
                context.actorOf(Props[MyTwitterHandler]) ! (event,response)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - Logout exception for: " + username)
                val failure = ts + " - Logout exception for: " + username
                context.actorOf(Props[MyTwitterHandler]) ! (event,failure)
          
        }
    case TWEETTAGMESSAGE(event) =>
        val tagmessage = event.readText.substring(event.readText.indexOf("#"), event.readText.length)
        val tag = tagmessage.substring(0, tagmessage.indexOf(" "))
        val message = tagmessage.substring(tagmessage.indexOf(" ") + 1, tagmessage.length)
        getUserName(event.webSocketId)
        Thread.sleep(20)
        addUserTweets(user, message)
        addTagTweets(tag,user, message)
        forwardFollowers(user, tagmessage)
        Thread.sleep(20)
        context.actorOf(Props[MyTwitterHandler]) ! TWEET(event.webSocketId, user + " - " + tagmessage + "\n")
       
       
      
    case TWEETMESSAGE(event) =>
        val message =  event.readText.substring(event.readText.indexOf("0") + 1, event.readText.length)
        getUserName(event.webSocketId)
        Thread.sleep(20)
        addUserTweets(user, message)
        forwardFollowers(user, message)
        Thread.sleep(20)
        context.actorOf(Props[MyTwitterHandler]) ! TWEET(event.webSocketId,user + " - " + message + "\n")
       
      
    case GETFOLLOWERS(event) =>
        val buf = new StringBuilder()
        getUserName(event.webSocketId)
        Thread.sleep(20)
        val future = networkHandler ? GETFOLLOWERSLIST(user)
        
        future.onSuccess{
          case FOLLOWERSLIST(followers) =>
                log.info(ts + " - GetFollowers successful")
                for(follower <- followers) {
                    buf.append("\n" + follower)
                  
                }
                context.actorOf(Props[MyTwitterHandler]) ! (event,buf.toString)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - GetFollowers failure")
                val failure = ts + " - GetFollowers failure"
                context.actorOf(Props[MyTwitterHandler]) ! (event,failure)         
        }
        
    case GETFOLLOWINGS(event) =>
        val buf = new StringBuilder()
        getUserName(event.webSocketId)
        Thread.sleep(20)
        val future = networkHandler ? GETFOLLOWINGSLIST(user)
        
        future.onSuccess{
          case FOLLOWERSLIST(followings) =>
                log.info(ts + " - GetFollowings successful")                
                for(followering <- followings)
                    buf.append("\n" + followering)
                context.actorOf(Props[MyTwitterHandler]) ! (event,buf.toString)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - GetFollowings failure")
                val failure = ts + " - GetFollowings failure"
                context.actorOf(Props[MyTwitterHandler]) ! (event,failure)
        }
    case GETUSERMESSAGES(event) =>
        val buf = new StringBuilder()
        val username = event.readText.substring(event.readText.indexOf("@"), event.readText.length)
        val future = tweetHandler ? GETUSERTWEETS(username)
      
        future.onSuccess{
          case TWEETS(tweets) =>
                log.info(ts + " - GetUserMessages successful")
                for(tweet <- tweets)
                    buf.append("\n" + username + " - " + tweet)
                context.actorOf(Props[MyTwitterHandler]) ! (event,buf.toString)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - GetUserMessages failure")
                val temp = ts + " - GetUserMessages failure"
                context.actorOf(Props[MyTwitterHandler]) ! (event,temp)          
        }
    case GETTAGMESSAGES(event) =>
        val buf = new StringBuilder()
        val tag = event.readText.substring(event.readText.indexOf("#"), event.readText.length)
        val future = tweetHandler ? GETTAGTWEETS(tag)
        
        
        future.onSuccess{
          case TWEETS(tweets) =>
                log.info(ts + " - GetTagMessages successful")
                for(tweet <- tweets)
                    buf.append("\n" + tag + " " + tweet)
                context.actorOf(Props[MyTwitterHandler]) ! (event,buf.toString)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - GetTagMessages failure")
                val temp = ts + " - GetTagMessages failure"
                context.actorOf(Props[MyTwitterHandler]) ! (event,temp)         
        }
    case STARTFOLLOWING(event) =>
        
        val following = event.readText.substring(event.readText.indexOf("@"), event.readText.length)
        getUserName(event.webSocketId)
        
        Thread.sleep(20)
        val future = networkHandler ? FOLLOWS(user, following)
        
        future.onSuccess{
          case FOLLOWINGS(response) =>
                log.info(ts + " - StartFollowing successful")
               
                context.actorOf(Props[MyTwitterHandler]) ! (event,response)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - StartFollowing failure")
                val temp = " - StartFollowing unsuccessful"
                context.actorOf(Props[MyTwitterHandler]) ! (event,temp)          
        }
    case STOPFOLLOWING(event) =>
       
       val following = event.readText.substring(event.readText.indexOf("@"), event.readText.length)
        getUserName(event.webSocketId)
        
        
        Thread.sleep(20)
        val future = networkHandler ? STOPFOLLOWS(user, following)
        
        future.onSuccess{
          case FOLLOWINGS(response) =>
                log.info(ts + " - StopFollowing successful")
  
                context.actorOf(Props[MyTwitterHandler]) ! (event,response)
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " messsaging failure")
                val temp = "Unfollow Operation unsuccessful"
                context.actorOf(Props[MyTwitterHandler]) ! (event,temp)
        }
  }
   
  private def addTagTweets(tag: String, username: String, tweet: String) {
        val future = tweetHandler ? ADDTAGTWEETS(tag, tweet)
        
        future.onSuccess{
          case TWEETRESPONSE(reponse) =>
                log.info(ts + " - Tweet connection successful for: " + username)               
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - Tag messsaging failure")               
        }   
  }
  
  private def addUserTweets(username: String, tweet: String) {
        val future = tweetHandler ? ADDUSERTWEETS(username, tweet)
        
        future.onSuccess{
          case TWEETRESPONSE(reponse) =>
                log.info(ts + " - Tweet connection successful for: " + username)                
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - Messsaging failure")                                   
        }
  }
  
  private def forwardFollowers(username: String, message: String) {
        val future = clientHandler ? FORWARDFOLLOWERS(username, networkHandler)
        user = "0" + username
        future.onSuccess{
          case FOLLOWERS(followers) =>
                log.info(ts + " - forwardFollowers successful for: " + username)
                for(follower <- followers)
                    context.actorOf(Props[MyTwitterHandler]) ! TWEET(follower, user + " - " + message + "\n")
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - Forwarding failure")        
        }
  }
  
  private def getUserName(webSocketID: String) {       
        val future = clientHandler ? GETUSERNAME(webSocketID)
      
        future.onSuccess{
          case USERNAME(username) =>
                user = username  
                log.info(ts + " - getUserName successful ")  
                      
        }
        future.onFailure{
          case exception: Exception =>
                log.info(ts + " - getUserName failure")                      
        } 

       
  }
}