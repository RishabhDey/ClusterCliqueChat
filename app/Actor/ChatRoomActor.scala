package Actor

import model.{ChatMessage, ChatRoom, Message, Offline, Online, Post, PostMessage, User, UserJoined, UserLeft}
import org.apache.pekko.actor.{Actor, ActorRef}
import model.JsonFormats._
import play.api.libs.json.{Json}

import java.time.Instant
import scala.collection.mutable




case class JoinRoom(user: User)
case class LeaveRoom(user: User)
case class sendChatMessage(user: User, chatMessage: ChatMessage)
case class sendPostMessage(user: User, postMessage: PostMessage)
case class getSnapshot()
case class GetMessages(limit: Int)
case class GetMessagesBefore(timestamp: Instant, limit: Int)
case class subscribe(actor: ActorRef)
case class unsubscribe(actor: ActorRef)

class ChatRoomActor(roomId: String, chatManager: ActorRef) extends Actor{
  private val MessageLimit = 90

  //These represent the actual users themselves
  private val members = mutable.HashMap[String, User]()
  private val messages = mutable.ArrayBuffer.empty[Message]

  //Actors within the class
  private var subscribers: Set[ActorRef] = Set.empty

  private def getRecentMessages(limit: Int = MessageLimit): Seq[Message] = {
    val count = math.min(limit, messages.size)
    if (count == 0) Seq.empty
    else messages.takeRight(count).toSeq
  }


  private def createSnapshot(): ChatRoom = {
    print("Printing Snapshot: ")
    val json = ChatRoom(members = members.values.toSeq, roomId = roomId, messages = getRecentMessages())
    println(Json.prettyPrint(Json.toJson(json)))
    print("Printed Snapshot")
    json
  }

  def receive: Receive = {
    case JoinRoom(user) =>
      println(s"Entering user ${user.userId}")
      members.update(user.userId, user.copy(status = Online()))
      sender() ! createSnapshot()
      broadcast(UserJoined(user))


    case LeaveRoom(user) =>
      members.update(user.userId, user.copy(status = Offline()))
      broadcast(UserLeft(user))



    case sendChatMessage(user, chatMessage) =>
      messages += chatMessage
      broadcast(chatMessage)

    case sendPostMessage(user, postMessage) =>
      messages += postMessage
      broadcast(postMessage)

    case getSnapshot() =>
      sender() ! createSnapshot()

    case GetMessages(limit) =>
      sender() ! getRecentMessages(limit)

    case GetMessagesBefore(timestamp, limit) =>
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
        chatManager ! RemoveRoom(roomId)
      }
  }

  private def broadcast(message: Any): Unit = {
    subscribers.foreach(_ ! message)
  }
}
