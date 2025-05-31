package Actor

import model.{ChatMessage, ChatRoom, Message, Offline, Online, Post, PostMessage, User, UserJoined, UserLeft, getMessages}
import org.apache.pekko.actor.{Actor, ActorRef}
import model.JsonFormats._
import play.api.libs.json.Json

import java.time.Instant
import scala.collection.mutable




case class JoinRoom(user: User)
case class LeaveRoom(user: User)
case class sendChatMessage(user: User, chatMessage: ChatMessage)
case class sendPostMessage(user: User, postMessage: PostMessage)
case class getSnapshot()
case class subscribe(actor: ActorRef)
case class unsubscribe(actor: ActorRef)
case class getMessagesMessage(timestamp: Instant, limit: Int)

class ChatRoomActor(roomId: String, chatManager: ActorRef, chatRoom: Option[ChatRoom] = None) extends Actor{


  private val MessageLimit = 90

  //These represent the actual users themselves
  private val members: mutable.HashMap[String, User] = chatRoom match {
    case Some(chatRoom) =>
      mutable.HashMap(chatRoom.members.map(user => user.userId -> user): _*)
    case None =>
      mutable.HashMap[String, User] ()
  }
  private val messages: mutable.ArrayBuffer[Message] = chatRoom match {
    case Some(chatRoom) =>
      mutable.ArrayBuffer[Message](chatRoom.messages: _*)
    case None =>
      mutable.ArrayBuffer[Message]()
  }


  //Actors within the class
  private var subscribers: Set[ActorRef] = Set.empty


  private def getRecentMessages(limit: Int = MessageLimit): Seq[Message] = {
    val count = math.min(limit, messages.size)
    if (count == 0) Seq.empty
    else messages.takeRight(count).toSeq
  }


  private def createSnapshot(): ChatRoom = {
    ChatRoom(members = members.values.toSeq, roomId = roomId, messages = getRecentMessages())
  }

  override def postStop(): Unit = {
    chatManager ! createSnapshot()
    chatManager ! RemoveRoom(roomId)
  }

  def receive: Receive = {
    case JoinRoom(user) =>
      println(s"Entering user ${user.userId}")
      members.update(user.userId, user.copy(status = Online()))
      sender() ! createSnapshot()
      broadcast(UserJoined(userJoined = user))

    case LeaveRoom(user) =>
      members.update(user.userId, user.copy(status = Offline()))
      broadcast(UserLeft(userLeft = user))

    case sendChatMessage(user, chatMessage) =>
      messages += chatMessage
      broadcast(chatMessage)

    case sendPostMessage(user, postMessage) =>
      messages += postMessage
      broadcast(postMessage)

    case getSnapshot() =>
      sender() ! createSnapshot()

    case getMessagesMessage(timestamp, limit) =>
      val messagesBeforeTime = messages
        .filter(_.dateTime.isBefore(timestamp))
        .takeRight(limit)
        .toSeq
      sender() ! messagesBeforeTime

    case subscribe(actor) =>
      subscribers += actor

    //if no websocket connections, remove the room
    case unsubscribe(actor) =>
      subscribers -= actor
      if (subscribers.isEmpty) {
        chatManager ! SaveRoom(roomId)
        chatManager ! RemoveRoom(roomId)
      }
  }

  private def broadcast(message: Any): Unit = {
    subscribers.foreach(_ ! message)
  }
}
