package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import Actor.{ChatManager, ChatUserActor}
import model.{ChatModel, ChatRoom, Online, User}
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.javadsl.Flow
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import views.html._

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
  def chat(roomId: String, userId: String) = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.chat(roomId, userId))
  }

  def chatSocket(roomId: String, userId: String) = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    Future.successful {
      val user = new User(userId, "www.example.com", status = Online())
      val flow = webSocketFlow(user, roomId)
      Right(flow)
    }
  }

  def webSocketFlow(user: User, roomId: String) = {
    ActorFlow.actorRef { out =>
      Props(new ChatUserActor(user, out, chatManager, roomId))
    }
  }

  def saveSnapshot(chatRoom: ChatRoom):Unit = {
     chatModel.saveSnapshot(chatRoom)
  }

}
