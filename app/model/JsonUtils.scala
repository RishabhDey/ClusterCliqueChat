package model

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, OFormat}
import JsonFormats._

import java.time.Instant

//View -> Controller
sealed trait JsonRequests {
  val typ: String
}
//Controller -> View
private[model] trait JsonRetrieve {
  val typ: String
}
object JsonRequests {
  def parseIncoming(json: JsValue): Option[JsonRequests] = {
    (json \ "typ").asOpt[String] match {
      case Some("sendChat")    =>
        json.validate[sendChat].asOpt

      case Some("sendPost")    =>
        json.validate[sendPost].asOpt
      case Some("getSnapshot") =>
        json.validate[getSnapshot].asOpt
      case Some("getMessages") =>
        json.validate[getMessages].asOpt
      case _                   =>
        print("Conversion Failed")
        None
    }
  }
}
sealed trait SendMessage

case class sendChat(override val typ: String = "sendChat", sendChatMessage: String) extends SendMessage with JsonRequests{
  require(typ == "sendChat", "typ must be 'sendChat'")
}

case class sendPost(override val typ: String = "sendPost", sendPostMessage: String) extends SendMessage with JsonRequests{
  require(typ == "sendPost", "typ must be 'sendPost'")
}

case class getSnapshot(override val typ: String = "getSnapshot") extends JsonRequests{
  require(typ == "getSnapshot", "typ must be 'getSnapshot'")
}

case class getMessages(override val typ: String = "getMessages", timestamp: Instant, limit: Int) extends JsonRequests{
  require(typ == "getMessages", "typ must be 'getMessages'")
}


object JsonFormats {
  //Hierarchical Classes need separate logic to separate them.
  implicit val formatStatus: Format[Status] = new Format[Status] {
    override def writes(status: Status): JsValue = status match {
      case Online() => JsString("online")
      case Offline() => JsString("offline")
    }
    override def reads(json: JsValue): JsResult[Status] = json match {
      case JsString("online") => JsSuccess(Online())
      case JsString("offline") => JsSuccess(Offline())
      case _ => JsError("Unknown Status")
    }
  }
  implicit val formatUser: OFormat[User] = Json.format[User]
  implicit val messageFormat: OFormat[Message] = Json.format[Message]
  implicit val userEventFormat: OFormat[UserEvent] = Json.format[UserEvent]
  implicit val formatChatRoom: OFormat[ChatRoom] = Json.format[ChatRoom]
  implicit val formatUserJoined: OFormat[UserJoined] = Json.format[UserJoined]
  implicit val formatUserLeft: OFormat[UserLeft] = Json.format[UserLeft]
  implicit val formatPost: OFormat[Post] = Json.format[Post]
  implicit val formatChatMessage: OFormat[ChatMessage] = Json.format[ChatMessage]
  implicit val formatPostMessage: OFormat[PostMessage] = Json.format[PostMessage]
  implicit val formatSendChat: OFormat[sendChat] = Json.format[sendChat]
  implicit val formatSendPost: OFormat[sendPost] = Json.format[sendPost]
  implicit val formatJsonRequest: OFormat[JsonRequests] = Json.format[JsonRequests]
  implicit val formatGetMessages: OFormat[getMessages] = Json.format[getMessages]
  implicit val formatGetSnapshot: OFormat[getSnapshot] = Json.format[getSnapshot]
  implicit val formatMessageBlock: OFormat[MessageBlock] = Json.format[MessageBlock]
}
