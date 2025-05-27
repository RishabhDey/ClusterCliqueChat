package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import Actor.{ChatManager, ChatUserActor}
import model.{ChatModel, ChatRoom, JsonRequests, Online, User}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source, SourceQueue}
import org.apache.pekko.stream.scaladsl.SourceQueueWithComplete
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import views.html._

import java.lang.System.console
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ChatController @Inject()(val controllerComponents: ControllerComponents)
                              (implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext)
  extends BaseController {

  private val chatManager: ActorRef = system.actorOf(Props(new ChatManager(this)), "ChatManager")
  private val chatModel = new ChatModel()

  def index = Action {
    Ok(views.html.index())
  }

  def chat(roomId: String, userId: String) = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.chat(roomId, userId))
  }

  def chatSocket(roomId: String, userId: String) = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    Future.successful {
      print("Successful Connection, Continuing")
      val user = new User(userId, "www.example.com", status = Online())
      val flow: Flow[JsValue, JsValue, NotUsed] = webSocketFlow(user, roomId)
      Right(flow): Either[Result, Flow[JsValue, JsValue, _]]
    }
  }

  def webSocketFlow(user: User, roomId: String) = {

    val (sourceQueue, source): (SourceQueueWithComplete[JsValue], Source[JsValue, _]) = Source.queue[JsValue](
      bufferSize = 16,
      overflowStrategy = OverflowStrategy.backpressure
    ).preMaterialize()


    val chatUserActor: ActorRef = system.actorOf(Props(new ChatUserActor(user, sourceQueue, chatManager, roomId)))

    val sink = Sink.foreach[JsValue] {jsValue =>
      JsonRequests.parseIncoming(jsValue) match {
        case Some(parsedMessage) =>
          chatUserActor ! parsedMessage
      }

    }
    Flow.fromSinkAndSource(sink, source)
  }

  def saveSnapshot(chatRoom: ChatRoom):Unit = {
     chatModel.saveSnapshot(chatRoom)
  }



}
