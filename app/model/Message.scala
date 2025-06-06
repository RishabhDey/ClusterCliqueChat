package model

import java.time.Instant
import java.util.UUID

sealed abstract class Message(val messageId: UUID = UUID.randomUUID(),
                              val messageStored: Boolean,
                              val user: User,
                              val dateTime: Instant)

case class ChatMessage(override val typ: String = "chat",
                       override val messageStored: Boolean = false,
                       override val user: User,
                       override val dateTime: Instant = Instant.now,
                       val chatMessage: String
                      ) extends Message(messageStored = messageStored, user = user, dateTime = dateTime) with JsonRetrieve{
  require(typ == "chat", "typ must be 'chat'")
}

case class PostMessage(override val typ: String = "post",
                       override val messageStored: Boolean = false,
                       override val user: User,
                       override val dateTime: Instant = Instant.now,
                       val postMessage: String = "A Post was made!",
                       val post: Post,
                      ) extends Message(messageStored = messageStored, user = user, dateTime = dateTime) with JsonRetrieve{
  require(typ == "post", "typ must be 'post'")
}

case class Post(imgURL: String) //temp --> change and add functionality to this later, Im lazy so it will only be a picture for now.

