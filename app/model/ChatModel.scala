package model
import java.time.Instant
import scala.collection.concurrent.TrieMap
import org.mongodb.scala._
import org.mongodb.scala.model.{Filters, Updates}
import play.api.libs.json.Json
import java.util.UUID
import scala.concurrent.ExecutionContext

//This should call to database later
class ChatModel()(implicit ec: ExecutionContext) {

  val mongoClient = MongoClient(APIKeys.MongoKey)
  val database: MongoDatabase = mongoClient.getDatabase("cluster0")
  val chatRoomCollection: MongoCollection[Document] = database.getCollection("ChatRooms")
  val messagesCollection: MongoCollection[Document] = database.getCollection("messages")

  //These classes are temporary, everything here will eventually be in the Database.

  //MONGO DB
  //maps RoomId -> ChatRoom
  private val allChats = TrieMap[String, ChatRoom]()



  // THESE SHOULD BE IN RELATIONAL DB
  //maps UserId -> RefreshToken
  private val refreshTokens = TrieMap[String, RefreshToken]()
  //maps UserId -> Users
  private val Users = TrieMap[String, User]()

  def getUser(userId: String) = {
    Users.get(userId) match {
      case Some(user) => user
      case None =>
        val user = User(userId, "www.example.com", Online())
        Users(userId) = user
        user
    }
  }

  def saveSnapshot(snapshot: ChatRoom): Unit = {
    allChats.update(snapshot.roomId, snapshot)
  }
  def getSnapshot(roomId: String): Option[ChatRoom] = {
    allChats.get(roomId)
  }
  def getRefreshToken(refreshTokenStr: String): Option[RefreshToken] = {
    refreshTokens.get(refreshTokenStr).filter(token => token.expiry.isAfter(Instant.now))
  }
  def setRefreshToken(userId: String): Option[RefreshToken] = {
    val token = AuthUtil.generateNewRefreshToken(getUser(userId))
    refreshTokens(token.refreshToken) = token
    refreshTokens.get(token.refreshToken)
  }
  def getNewAccessToken(userId: String, refreshTokenStr: String): Option[String] = {
    getRefreshToken(refreshTokenStr).filter(_.user.userId.equals(userId)).map{_ =>
      AuthUtil.generateNewAccessToken(getUser(userId))
    }
  }

  def writeChatRoomToMB(chatRoom: ChatRoom) = {
    val blockIds = chatRoom.messages.filter(!_.messageStored).grouped(100).map { messages =>
      val id = UUID.randomUUID()
      val block = MessageBlock(id, messages)
      messagesCollection.insertOne(Document(block.toString())).toFuture()
      id
    }.toSeq

    val filter = Filters.eq("roomId", chatRoom.roomId)

    chatRoomCollection.find(filter).first().headOption().flatMap {
      case Some(_) =>
        val update = Updates.pushEach("messageChunks", blockIds)
        chatRoomCollection.updateOne(filter, update).toFuture()
      case None =>
        val chatRoomDoc = Json.obj(
          "_id" -> chatRoom.roomId,
          "typ" -> "chatRoom",
          "members" -> Json.toJson(chatRoom.members.map(_.userId)),
          "messageChunks" -> Json.toJson(blockIds)
        )
        chatRoomCollection.insertOne(Document(chatRoomDoc.toString())).toFuture()
    }

  }

  def getMessageBlockFromDB(roomId: String, lastTakenMessageIndex: Option[UUID]):Option[MessageBlock] = {
      ???
  }



  def readChatRoomFromDB = {
    ???
  }



}


//Database Transfer Use
case class ChatRoom(override val typ: String = "chatRoom", members: Seq[User], roomId: String, messages: Seq[Message]) extends JsonRetrieve{
  require(typ == "chatRoom", "typ must be 'chatRoom'")
}
case class MessageBlock(id: UUID, messages: Seq[Message])
