package model

import java.time.Instant

trait JsonRequests {
  val typ: String
}
sealed trait SendMessage

case class sendChat(override val typ: String = "sendChat", sendChatMessage: String) extends SendMessage with JsonRequests

case class sendPost(override val typ: String = "sendPost", sendPostMessage: String) extends SendMessage with JsonRequests

case class getSnapshot(override val typ: String = "getSnapshot") extends JsonRequests

case class getMessages(override val typ: String = "getMessages", timestamp: Instant, limit: Int) extends JsonRequests

