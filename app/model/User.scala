package model
import java.net.URL

sealed trait Status
case class Online() extends Status
case class Offline() extends Status

sealed trait UserEvent
case class UserJoined(user: User) extends UserEvent
case class UserLeft(user: User) extends UserEvent

case class User(userId: String, profileURL: String, status: Status)

