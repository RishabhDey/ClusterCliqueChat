package model

import java.time.Instant

sealed abstract class Message(val user: User, val dateTime: Instant)

case class ChatMessage(override val user: User,
                       override val dateTime: Instant = Instant.now,
                       val message: String
                      ) extends Message(user, dateTime)

case class PostMessage(override val user: User,
                       override val dateTime: Instant = Instant.now,
                       val message: String = "A Post was made!",
                       val post: Post,
                      ) extends Message(user, dateTime)

case class Post(imgURL: String) //temp --> change and add functionality to this later, Im lazy so it will only be a picture for now.

sealed trait SendMessage
case class sendChat(chatMessage: ChatMessage) extends SendMessage
case class sendPost(postMessage: PostMessage) extends SendMessage