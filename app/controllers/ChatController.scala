package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import Actor.{ChatManager, ChatUserActor, CreateUserActor}
import model.{AuthUtil, ChatModel, ChatRoom, JsonRequests, Online, User}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source, SourceQueue}
import org.apache.pekko.stream.scaladsl.SourceQueueWithComplete
import org.apache.pekko.util.Timeout
import play.api.libs.json.{JsValue, Json}
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


  //Woah, I just learned you can do this, instead of nesting Options with case Some(), you can use a for loop and it will
  // do it for you and then you can yield it!
  def refresh: Action[AnyContent] = Action { request =>
    val maybeNewAccessToken = for {
      cookie <- request.cookies.get("refreshToken")
      refreshToken <- chatModel.getRefreshToken(cookie.value)
      accessToken <- chatModel.getNewAccessToken(refreshToken.user.userId, refreshToken.refreshToken)
    } yield accessToken
    maybeNewAccessToken match {
      case Some(token) => Ok(Json.obj("accessToken" -> token))
      case None => Redirect("/login")
    }
  }

  def index: Action[AnyContent] = Action {
    Ok(views.html.index())
  }

  def chat(roomId: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.chat(roomId, ???))
  }


  def chatSocket(roomId: String) = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    def chatConnection(userId: String, roomId: String) = {
      val user = chatModel.getUser(userId)
      implicit val timeout: Timeout = 3.seconds
      val flowFuture = (chatManager ? CreateUserActor(user, roomId)).mapTo[Flow[JsValue, JsValue, _]]
      flowFuture.map(flow => Right(flow)).recover {
        case ex =>
          Left(InternalServerError("WebSocket error"))
      }
    }
    request.getQueryString("token") match {
      case Some(token) =>
        AuthUtil.validateUserToken(token) match {
          case Some(userId) =>
            chatConnection(userId, roomId)
          case None =>
            Future.successful(Left(Unauthorized("Invalid JWT")))
        }
      case None =>
        Future.successful(Left(Unauthorized("Missing token")))
    }
  }

  def saveSnapshot(chatRoom: ChatRoom):Unit = {
     chatModel.saveSnapshot(chatRoom)
  }

  def getSnapshot(roomId: String): Option[ChatRoom] = {
    chatModel.getSnapshot(roomId)
  }



}
