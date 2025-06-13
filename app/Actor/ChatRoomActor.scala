package Actor

import model.{ChatMessage, ChatRoom, Message, MessageBlock, Offline, Online, Post, PostMessage, User, UserJoined, UserLeft, getMessages}
import org.apache.pekko.actor.{Actor, ActorRef}
import model.JsonFormats._
import play.api.libs.json.Json

import java.time.Instant
import java.util.UUID
import scala.collection.mutable




case class JoinRoom(user: User)
case class LeaveRoom(user: User)
case class sendChatMessage(user: User, chatMessage: ChatMessage)
case class sendPostMessage(user: User, postMessage: PostMessage)
case class getSnapshot()

case class saveSnapshot()
case class subscribe(actor: ActorRef)
case class unsubscribe(actor: ActorRef)
case class getMessagesMessage(timestamp: Instant, limit: Int, request: Option[ActorRef] = None)

case class getMessageBlock(roomId: String, lastTakenMessageIndex: Option[Instant], requestUser: Option[ActorRef])

case class RecievedMessageBlock(messages: MessageBlock, request: Option[ActorRef])



/*
Every specific chatRoom has their own specific chat room
actor. This actor controls all the information that extends
to the users too. This can be classified as temporary storage
for easier access.
 */
class ChatRoomActor(roomId: String, chatManager: ActorRef, chatRoom: Option[ChatRoom] = None, timeStamp: Option[Instant] = None) extends Actor{


  private val MessageLimit = 30

  private var lastTakenMessageTimeStamp = timeStamp



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

    if (limit == 0) return Seq.empty
    else if(limit <= messages.size) messages.takeRight(limit).toSeq
    else{
      chatManager ! getMessageBlock(roomId, lastTakenMessageTimeStamp, None)
    }
    val count = math.min(limit, messages.size)
    if (count == 0) Seq.empty
    else messages.takeRight(count).toSeq
  }


  private def createSnapshot(msg_idx: Option[Int] = None): ChatRoom = {
    msg_idx match {
      case Some(idx) =>
        ChatRoom(members = members.values.toSeq, roomId = roomId, messages = getRecentMessages(idx))
      case None =>
        ChatRoom(members = members.values.toSeq, roomId = roomId, messages = getRecentMessages())
    }

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

    case saveSnapshot() =>
      chatManager ! createSnapshot(Some(messages.size))

    case getSnapshot() =>
      sender() ! createSnapshot()

    case getMessagesMessage(timestamp, limit, request) =>
      val messagesBeforeTime = messages
        .filter(_.dateTime.isBefore(timestamp))
        .takeRight(limit)
        .toSeq
      if(messagesBeforeTime.size < 30){
        chatManager ! getMessageBlock(roomId = roomId,
          lastTakenMessageIndex = lastTakenMessageTimeStamp,
          requestUser = Some(sender()))
      }else {
        request match {
          case Some(request) =>
            request ! messagesBeforeTime
          case None =>
            sender() ! messagesBeforeTime
        }

      }

    case subscribe(actor) =>
      subscribers += actor

    //if no websocket connections, remove the room
    case unsubscribe(actor) =>
      subscribers -= actor
      if (subscribers.isEmpty) {
        chatManager ! SaveRoom(roomId)
        chatManager ! RemoveRoom(roomId)
      }

    case messageBlock: MessageBlock =>
      messages.prependAll(messageBlock.messages)
      lastTakenMessageTimeStamp = Some(messageBlock.timeStamp)

    case RecievedMessageBlock(messageBlock, o_sender) =>
      messages.prependAll(messageBlock.messages)
      val currTimeStamp = lastTakenMessageTimeStamp
      lastTakenMessageTimeStamp = Some(messageBlock.timeStamp)
      currTimeStamp match {
        case Some(currTS) =>
          self ! getMessagesMessage(currTS,MessageLimit, o_sender)
      }
  }
  private def broadcast(message: Any): Unit = {
    subscribers.foreach(_ ! message)
  }
}
