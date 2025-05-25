package model

trait JsonRequests {
  val typ: String
}
sealed trait SendMessage

case class sendChat(override val typ: String = "sendChat", sendChatMessage: String) extends SendMessage with JsonRequests

case class sendPost(override val typ: String = "sendPost", sendPostMessage: String) extends SendMessage with JsonRequests


