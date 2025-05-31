package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import Actor.{ChatManager, ChatUserActor, CreateUserActor}
import model.{ChatModel, ChatRoom, JsonRequests, Online, User}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source, SourceQueue}
import org.apache.pekko.stream.scaladsl.SourceQueueWithComplete
import org.apache.pekko.util.Timeout
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import views.html._

import java.lang.System.console
import java.net.URL
import scala.concurrent.duration.DurationInt
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

  def chat(roomId: String) = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.chat(roomId, ???))
  }

  def chatSocket(roomId: String) = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    val userId = ???
    val user = new User(userId, "www.example.com", status = Online())
    implicit val timeout: Timeout = 3.seconds
    val flowFuture = (chatManager ? CreateUserActor(user, roomId)).mapTo[Flow[JsValue, JsValue, _]]
    flowFuture.map(flow => Right(flow)).recover {
      case ex =>
        Left(InternalServerError("WebSocket error"))
    }
  }

  def saveSnapshot(chatRoom: ChatRoom):Unit = {
     chatModel.saveSnapshot(chatRoom)
  }

  def getSnapshot(roomId: String): Option[ChatRoom] = {
    chatModel.getSnapshot(roomId)
  }



}
