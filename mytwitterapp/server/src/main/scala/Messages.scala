package MyTwitterApp


import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.actor.ActorRef
import scala.collection.mutable.Set



    case class LOGIN(event: WebSocketFrameEvent) 
    case class LOGOUT(event: WebSocketFrameEvent) 
    case class TWEETTAGMESSAGE(event: WebSocketFrameEvent) 
    case class TWEETMESSAGE(event: WebSocketFrameEvent) 
    case class GETFOLLOWERS(event: WebSocketFrameEvent) 
    case class GETFOLLOWINGS(event: WebSocketFrameEvent) 
    case class GETUSERMESSAGES(event: WebSocketFrameEvent) 
    case class GETTAGMESSAGES(event: WebSocketFrameEvent) 
    case class STARTFOLLOWING(event: WebSocketFrameEvent) 
    case class STOPFOLLOWING(event: WebSocketFrameEvent) 
    case class TWEET(username: String,tweet: String)
    
    case class CLIENTCONNECT(webSocketID: String, username: String,networkHandler: ActorRef)
    case class CONNECTRESPONSE(response: String)
    case class CLIENTDISCONNECT(webSocketID: String)
    case class DISCONNECTRESPONSE(response: String)
    case class GETFOLLOWERSLIST(username: String)
    case class FOLLOWERSLIST(followers: Set[String])
    case class GETFOLLOWINGSLIST(username: String)
    case class FOLLOWINGSLIST(followings: Set[String])
    case class GETUSERTWEETS(username: String)
    case class TWEETS(tweets: List[String])
    case class GETTAGTWEETS(tag: String)
    case class FOLLOWS(username: String, following: String)
    case class FOLLOWINGS(response: String)
    case class STOPFOLLOWS(username: String, following: String)
    case class ADDTAGTWEETS(tag: String, tweet: String)
    case class ADDUSERTWEETS(username: String, tweet: String)
    case class TWEETRESPONSE(reponse: String)
    case class FORWARDFOLLOWERS(username: String, networkHandler: ActorRef)
    case class FOLLOWERS(followers: List[String])
    case class GETUSERNAME(webSocketID: String)
    case class USERNAME(username: String)
    
    case class NETWORKCONNECT(username: String)
    case class FOLLOWERINGSLIST(follwering: Set[String])
    case class FdOLLOWERINGSLIST(follwering: Set[String])
    