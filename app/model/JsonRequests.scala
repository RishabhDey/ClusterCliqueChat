package model

import play.api.libs.json.{JsValue, Json, OFormat}
import JsonFormats._
import java.time.Instant


object JsonRequests {
  def parseIncoming(json: JsValue): Option[JsonRequests] = {
    print(json)
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
//View -> Controller
sealed trait JsonRequests {
   val typ: String
}
//Controller -> View
private[model] trait JsonRetrieve {
  val typ: String
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

