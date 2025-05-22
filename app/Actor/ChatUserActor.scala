package Actor

import model.{ChatMessage, ChatRoom, Message, Online, Post, PostMessage, User, UserJoined, UserLeft, sendChat, sendPost}
import model.JsonFormats._
import org.apache.pekko.actor.typed.RecipientRef
import org.apache.pekko.actor.{Actor, ActorRef}
import org.apache.pekko.util.Timeout
import play.api.libs.json.{Format, JsError, JsObject, JsResult, JsString, JsSuccess, JsValue, Json, OFormat}
import org.apache.pekko.pattern.ask

import java.net.URL
import scala.concurrent.duration.DurationInt





class ChatUserActor(user: User, out: ActorRef, ChatManager: ActorRef, roomRef: String) extends Actor{

  override def preStart(): Unit = {
    ChatManager ! GetRoom(roomRef)
  }

  override def receive: Receive = waitingRoom

  private def waitingRoom: Receive = {
    case RoomRef(roomRef) =>
      roomRef ! subscribe(self)
      roomRef ! JoinRoom(user)
      roomRef ! getSnapshot()
      context.become(ChatOpen(roomRef))

    case other =>
      println(s"ChatUserActor waiting for room — ignored message: $other")
  }

  def ChatOpen(roomRef: ActorRef): Receive = {
    case sendChat(chatMessage) =>
      roomRef ! sendChatMessage(user, chatMessage)

    case sendPost(postMessage) =>
      roomRef ! sendPostMessage(user, postMessage)

    case getSnapshot() =>
      roomRef ! getSnapshot()

    case GetMessages(limit) =>
      roomRef ! GetMessages(limit)

    case GetMessagesBefore(timestamp, limit) =>
      roomRef ! GetMessagesBefore(timestamp, limit)

    case snapshot: ChatRoom =>
      out ! Json.toJson(snapshot)

    case msgs: Seq[_] if msgs.forall(_.isInstanceOf[Message]) =>
      val typedMsgs = msgs.asInstanceOf[Seq[Message]]
      out ! Json.toJson(typedMsgs)

    case msg: UserJoined =>
      out ! Json.toJson(msg)

    case msg: UserLeft =>
      out ! Json.toJson(msg)

    case msg: ChatMessage =>
      out ! Json.toJson(msg)

    case msg: PostMessage =>
      out ! Json.toJson(msg)

    case unknown =>
      println(s"ChatUserActor received unknown message: $unknown")
  }

  override def postStop(): Unit = {
    context.children.headOption.foreach(_ ! LeaveRoom(user))
    context.children.headOption.foreach(_ ! unsubscribe(self))
  }
}
