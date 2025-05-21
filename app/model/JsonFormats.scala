package model

import play.api.libs.json.{Format, JsError, JsObject, JsResult, JsString, JsSuccess, JsValue, Json, JsonValidationError, OFormat, OWrites, Reads, Writes}


/*Json.toJson requires implicits to convert from Scala Objects to JSON files. I opted to collate all
of them in one area, but this can def be abstracted as you wish.
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
  implicit val messageFormat: Format[Message] = new Format[Message] {
    override def writes(msg: Message): JsValue = msg match {
      case cm: ChatMessage => formatChatMessage.writes(cm).as[JsObject] + ("type" -> JsString("chat"))
      case pm: PostMessage => formatPostMessage.writes(pm).as[JsObject] + ("type" -> JsString("post"))
    }
    override def reads(json: JsValue): JsResult[Message] = (json \ "type").validate[String].flatMap {
      case "chat" => formatChatMessage.reads(json)
      case "post" => formatPostMessage.reads(json)
      case _ => JsError("Unknown message type")
    }
  }


  //Increased Size as SendChat should only have ChatMessage and same for PostChat
  implicit val formatSendMessage: Format[SendMessage] = new Format[SendMessage] {
    override def writes(msg: SendMessage): JsValue = msg match {
      case sc: sendChat => formatSendChat.writes(sc).as[JsObject] + ("type" -> JsString("sendChat"))
      case sp: sendPost => formatSendPost.writes(sp).as[JsObject] + ("type" -> JsString("sendPost"))
    }

    override def reads(json: JsValue): JsResult[SendMessage] = (json \ "type").validate[String].flatMap {
      case "sendChat" =>
        (json \ "chatMessage").validate[ChatMessage].flatMap { cm =>
          JsSuccess(sendChat(cm))
        }.recoverWith(_ => JsError("Missing or invalid 'chatMessage' field for sendChat"))

      case "sendPost" =>
        (json \ "postMessage").validate[PostMessage].flatMap { pm =>
          JsSuccess(sendPost(pm))
        }.recoverWith(_ => JsError("Missing or invalid 'postMessage' field for sendPost"))
      case other =>
        JsError(s"Unknown SendMessage type: $other")
    }
  }

  implicit val formatUserEvent: Format[UserEvent] = new Format[UserEvent] {
    override def writes(event: UserEvent): JsValue = event match {
      case uj: UserJoined => formatUserJoined.writes(uj).as[JsObject] + ("type" -> JsString("UserJoined"))
      case ul: UserLeft => formatUserLeft.writes(ul).as[JsObject] + ("type" -> JsString("UserLeft"))
    }

    override def reads(json: JsValue): JsResult[UserEvent] = (json \ "type").validate[String].flatMap {
      case "UserJoined" =>
        val jsonWithoutType = json.as[JsObject] - "type"
        formatUserJoined.reads(jsonWithoutType)
      case "UserLeft" =>
        val jsonWithoutType = json.as[JsObject] - "type"
        formatUserLeft.reads(jsonWithoutType)
      case other => JsError(s"Unknown user event type: $other")
    }
  }



  implicit val readTypedChatRoom: Reads[ChatRoom] = (json: JsValue) => {
    (json \ "type").validate[String].flatMap {
      case "ChatRoom" =>
        val jsonWithoutType = json.as[JsObject] - "type"
        formatBaseChatRoom.reads(jsonWithoutType)
      case other => JsError(s"Unknown Type: $other")
    }
  }

  implicit val writeTypedChatRoom: OWrites[ChatRoom] = (chatRoom: ChatRoom) => {
    formatBaseChatRoom.writes(chatRoom).as[JsObject] + ("type" -> JsString("ChatRoom"))
  }
  implicit val formatBaseChatRoom: OFormat[ChatRoom] = Json.format[ChatRoom]
  implicit val formatTypedChatRoom: OFormat[ChatRoom] = OFormat(readTypedChatRoom, writeTypedChatRoom)

  implicit val formatUser: OFormat[User] = Json.format[User]
  implicit val formatUserJoined: OFormat[UserJoined] = Json.format[UserJoined]
  implicit val formatUserLeft: OFormat[UserLeft] = Json.format[UserLeft]



  implicit val formatPost: OFormat[Post] = Json.format[Post]
  implicit val formatChatMessage: OFormat[ChatMessage] = Json.format[ChatMessage]
  implicit val formatPostMessage: OFormat[PostMessage] = Json.format[PostMessage]
  implicit val formatSendChat: OFormat[sendChat] = Json.format[sendChat]
  implicit val formatSendPost: OFormat[sendPost] = Json.format[sendPost]


}
