package Actor

import controllers.ChatController
import model.ChatRoom
import org.apache.pekko.actor.{Actor, ActorRef, Cancellable, Props}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class GetRoom(roomId: String)
case class RoomRef(ref: ActorRef)
case class RemoveRoom(roomId: String)
case class SaveRooms(roomId: String)
case class getRoomSnapshot(roomId: String)
//The chatManager is essentially the websocket "model" for all the chat system, it sends everything to the model periodically to store.

class ChatManager(chatController: ChatController) extends Actor{

  implicit val ec: ExecutionContext = context.system.dispatcher

  private val saveScheduler: Cancellable =
    context.system.scheduler.scheduleWithFixedDelay(5.minutes, 5.minutes, self, SaveRooms)


  private val rooms = mutable.HashMap[String, ActorRef]()
  def receive: Receive = {
    case GetRoom(roomId) =>
      val roomActor = rooms.getOrElseUpdate(roomId, context.actorOf(Props(new ChatRoomActor(roomId, self)), s"chatRoom-$roomId"))
      sender() ! RoomRef(roomActor)


    //Add save room functionality later by calling controller and adding to model.
    case RemoveRoom(roomId) =>
      rooms.remove(roomId).foreach { actor =>
        println(s"[ChatManager] Stopping room $roomId")
        context.stop(actor)
      }

    case getRoomSnapshot(roomId) =>
        rooms(roomId) ! getSnapshot()

    case chatRoom: ChatRoom =>
      chatController.saveSnapshot(chatRoom)

    case unknown =>
      println(s"[ChatManager] Received unknown message: $unknown")

  }
}

