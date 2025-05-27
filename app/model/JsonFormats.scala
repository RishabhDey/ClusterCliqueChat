package model

import play.api.libs.json.{Format, JsError, JsObject, JsResult, JsString, JsSuccess, JsValue, Json, JsonValidationError, OFormat, OWrites, Reads, Writes, __}


/*Json.toJson requires implicits to convert from Scala Objects to JSON files. I opted to collate all
of them in one area, but this can def be abstracted as you wish.

Most def abstract the type fields, honestly I should've done that, would've saved me a headache and a half.z

Edit: My intial method was dumb lmao, i was adding an additional type field when i can just have unique parameter names lmao.

 */



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
}
