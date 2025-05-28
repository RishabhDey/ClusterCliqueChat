package model
import java.net.URL

sealed trait Status
case class Online() extends Status
case class Offline() extends Status

sealed trait UserEvent
case class UserJoined(override val typ: String = "userJoined", userJoined: User) extends UserEvent with JsonRetrieve {
  require(typ == "userJoined", "typ must be 'userJoined'")
}
case class UserLeft(override val typ: String = "userLeft", userLeft: User) extends UserEvent with JsonRetrieve {
  require(typ == "userLeft", "typ must be 'userLeft'")
}

case class User(userId: String, profileURL: String = "N/A", status: Status = Offline())

