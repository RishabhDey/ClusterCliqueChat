package Actor

import model.{ChatMessage, ChatRoom, Message, Online, Post, PostMessage, User, UserJoined, UserLeft, getMessages, sendChat, sendPost}
import model.JsonFormats._
import org.apache.pekko.actor.typed.RecipientRef
import org.apache.pekko.actor.{Actor, ActorRef}
import org.apache.pekko.util.Timeout
import play.api.libs.json.{Format, JsError, JsObject, JsResult, JsString, JsSuccess, JsValue, Json, OFormat}
import org.apache.pekko.pattern.ask

import java.net.URL
import scala.concurrent.duration.DurationInt





class ChatUserActor(user: User, out: ActorRef, ChatManager: ActorRef, roomRefStr: String) extends Actor{
  import context.dispatcher
  implicit val timeout: Timeout = 3.seconds

  private var roomActorRef: Option[ActorRef] = None

  override def preStart(): Unit = {
    val roomFuture = (ChatManager ? GetRoom(roomRefStr)).mapTo[RoomRef]
    roomFuture.foreach { roomRefMsg =>
      self ! roomRefMsg
    }
  }

  override def receive: Receive = waitingRoom

  private def waitingRoom: Receive = {
    case RoomRef(roomRef) =>
      roomActorRef = Some(roomRef)
      roomRef ! subscribe(self)
      roomRef ! JoinRoom(user)
      roomRef ! getSnapshot()
      context.become(ChatOpen(roomRef))

    case other =>
      println(s"ChatUserActor waiting for room — ignored message: $other")
  }

  def ChatOpen(roomRef: ActorRef): Receive = {
    case sendChat(_, message) =>
      roomRef ! sendChatMessage(user, ChatMessage(user = user, chatMessage = message))

    case sendPost(_, post) =>
      roomRef ! sendPostMessage(user, PostMessage(user = user, post = Post(post)))

    case getSnapshot() =>
      roomRef ! getSnapshot()

    case getMessages(_, timestamp, limit) =>
      roomRef ! getMessagesMessage(timestamp, limit)

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
    roomActorRef match {
      case Some(ref) =>
        ref ! LeaveRoom(user)
        ref ! unsubscribe(self)
      case None =>
    }
  }
}
