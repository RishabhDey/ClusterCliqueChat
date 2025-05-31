package Actor

import controllers.ChatController
import model.{ChatRoom, JsonRequests, User}
import org.apache.pekko.actor.{Actor, ActorRef, Cancellable, Props}
import model.JsonFormats._
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.apache.pekko.util.Timeout
import play.api.libs.json.JsValue

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class GetRoom(roomId: String)
case class RoomRef(ref: ActorRef)
case class RemoveRoom(roomId: String)
case class SaveRooms()
case class SaveRoom(roomId: String)
case class getRoomSnapshot(roomId: String)

case class CreateUserActor(user: User, roomId: String)

//The chatManager is essentially the websocket "model" for all the chat system, it sends everything to the model periodically to store.

class ChatManager(chatController: ChatController)(implicit mat: Materializer) extends Actor{

  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout: Timeout = 3.seconds
  private val saveScheduler: Cancellable =
    context.system.scheduler.scheduleWithFixedDelay(5.minutes, 5.minutes, self, SaveRooms())


  private val rooms = mutable.HashMap[String, ActorRef]()
  def receive: Receive = {


    case GetRoom(roomId) =>
      println(s"[ChatManager] Starting room $roomId")
      val getChatRoom: Option[ChatRoom] = chatController.getSnapshot(roomId)
      val roomActor = rooms.getOrElseUpdate(roomId,
        context.actorOf(
          Props(new ChatRoomActor(roomId, self, getChatRoom)), s"chatRoom-$roomId")

      )

      sender() ! RoomRef(roomActor)


    //Add save room functionality later by calling controller and adding to model.
    case RemoveRoom(roomId) =>
      rooms.remove(roomId).foreach { actor =>
        println(s"[ChatManager] Stopping room $roomId")
        context.stop(actor)
      }
    case SaveRooms() =>
      rooms.keys.foreach{id =>
        self ! SaveRoom(id)
      }

    case CreateUserActor(user, roomId) =>
      val (queue, source) = Source.queue[JsValue](16, OverflowStrategy.backpressure).preMaterialize()
      val actorRef = context.actorOf(Props(new ChatUserActor(user, queue, self, roomId)))
      val sink = Sink.foreach[JsValue] { jsValue =>
        JsonRequests.parseIncoming(jsValue).foreach(actorRef ! _)
      }
      println("[ChatManager] Making Flow")
      sender() ! Flow.fromSinkAndSourceCoupledMat(sink, source)(Keep.both).watchTermination() { case ((_, _), termination) =>
        termination.onComplete { _ =>
          println(s"[ChatManager] WebSocket closed, stopping actor for ${user.userId}")
          context.stop(actorRef)
        }
      }

    case SaveRoom(roomId) =>
      println(s"[ChatManager] - ${roomId} is being Saved.")
        rooms(roomId) ! getSnapshot()

    case chatRoom: ChatRoom =>
      println(s"[ChatManager] - A ChatRoom was Sent, Saving.")
      chatController.saveSnapshot(chatRoom)

    case unknown =>
      println(s"[ChatManager] Received unknown message: $unknown")

  }

  override def postStop(): Unit = {
    saveScheduler.cancel()
    super.postStop()
  }
}

