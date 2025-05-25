package model

import java.time.Instant

sealed abstract class Message(val user: User, val dateTime: Instant)

case class ChatMessage(override val user: User,
                       override val dateTime: Instant = Instant.now,
                       val chatMessage: String
                      ) extends Message(user, dateTime)

case class PostMessage(override val user: User,
                       override val dateTime: Instant = Instant.now,
                       val postMessage: String = "A Post was made!",
                       val post: Post,
                      ) extends Message(user, dateTime)

case class Post(imgURL: String) //temp --> change and add functionality to this later, Im lazy so it will only be a picture for now.

