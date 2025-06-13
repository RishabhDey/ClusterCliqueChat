package model
import java.time.Instant
import scala.collection.concurrent.TrieMap
import org.mongodb.scala._
import org.mongodb.scala.model.{Filters, Indexes, Sorts, Updates}
import play.api.libs.json.{JsObject, Json}
import JsonFormats._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

//This should call to database later
class ChatModel()(implicit ec: ExecutionContext) {

  val mongoClient = MongoClient(APIKeys.MongoKey)
  val database: MongoDatabase = mongoClient.getDatabase("cluster0")
  val chatRoomCollection: MongoCollection[Document] = database.getCollection("ChatRooms")
  val messagesCollection: MongoCollection[Document] = database.getCollection("messages")
  messagesCollection.createIndex(Indexes.ascending("roomId", "timestamp"))

  //These classes are temporary, everything here will eventually be in the Database.

  //MONGO DB
  //maps RoomId -> ChatRoom
  private val allChats = TrieMap[String, ChatRoom]()
  // THESE SHOULD BE IN RELATIONAL DB
  //maps UserId -> RefreshToken
  private val refreshTokens = TrieMap[String, RefreshToken]()
  //maps UserId -> Users
  private val Users = TrieMap[String, User]()

  //maps RSDB Clique Id -> MongoDB RoomId
  private val RoomCliqueConnection = TrieMap[String, String]()

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
    val blockIds = chatRoom.messages.filter(!_.messageStored).grouped(30).map { messages =>
      val id = UUID.randomUUID()
      val block = MessageBlock(messageId = id, roomId = chatRoom.roomId, messages = messages, timeStamp = messages.head.dateTime)
      messagesCollection.insertOne(Document(block.toString())).toFuture()
      id
    }.toSeq

    val filter = Filters.eq("_id", chatRoom.roomId)

    chatRoomCollection.find(filter).first().headOption().flatMap {
      case Some(_) =>
        val update = Updates.addToSet("messageBlocks", blockIds)
        chatRoomCollection.updateOne(filter, update).toFuture()
      case None =>
        val chatRoomDoc = Json.obj(
          "_id" -> chatRoom.roomId,
          "typ" -> "chatRoom",
          "members" -> Json.toJson(chatRoom.members.map(_.userId)),
          "messageBlocks" -> Json.toJson(blockIds)
        )
        chatRoomCollection.insertOne(Document(chatRoomDoc.toString())).toFuture()
    }
  }


  //Call if user not in chatRoomCollection for some reason but in clique for some reason, this should be used as a backup

  def verifyUser(roomId: String, userId: String):Future[Boolean] = {
    val query = Document(
      "_id"->roomId,
      "members" -> userId
    )
    chatRoomCollection.find(query).first().toFuture().map(_!=null)
  }

  //IMPORTANT -- This must be called whenever a user joins a clique, in other words. Everything kinda depends on this.
  def addUser(roomId: String, userId: String): Future[result.UpdateResult] = {
    val update = Updates.addToSet("members", Json.toJson(userId))
    val filter = Filters.eq("_id", roomId)
    chatRoomCollection.updateOne(filter, update).toFuture()
  }

  //Same as above.
  def removeUser(roomId: String, userId: String): Future[result.UpdateResult] = {
    val update = Updates.pullAll("members", Json.toJson(userId))
    val filter = Filters.eq("_id", roomId)
    chatRoomCollection.updateOne(filter, update).toFuture()
  }


  /*
  Returns the most updated Chatroom alongside the most recently obtained messages from the
   */
  def readChatRoomFromDB(cliqueId: String) = {
    val roomId: String = RoomCliqueConnection(cliqueId)
    val filter = Filters.eq("_id", roomId)
    chatRoomCollection.find(filter).first().toFutureOption().flatMap {
      case Some(chatRoom) =>
        val jsObj: JsObject = Json.parse(chatRoom.toJson).as[JsObject]
        val memberIds = (jsObj \ "members").as[Seq[String]]
        val blockIds = (jsObj \ "messageBlocks").as[Seq[String]]
        val members = memberIds.map(getUser)
        if(blockIds.isEmpty){
          Future.successful((Some(ChatRoom(members = members, roomId = roomId, messages = Seq.empty)), None))
        }else{
          readMessageBlockFromDB(roomId, None).flatMap {
            case Some(messageBlock) =>
              Future.successful((Some(ChatRoom(roomId = roomId, members = members, messages = messageBlock.messages)), Some(messageBlock.timeStamp)))
            case None =>
              Future.successful((Some(ChatRoom(members = members, roomId = roomId, messages = Seq.empty)), None))
          }
        }
      case None =>
        Future.successful((None,None))
    }
  }

  def readMessageBlockFromDB(roomId: String, previousTimeStamp: Option[Instant]) = {
    val roomFilter = Filters.eq("roomId", roomId)
    val combinedFilters = previousTimeStamp match {
      case Some(pt) =>
        Filters.and(roomFilter, Filters.lt("timestamp", pt) )
      case None =>
        roomFilter
    }
    messagesCollection.find(combinedFilters).sort(Sorts.descending()).first().toFutureOption().flatMap{
      case Some(messages) =>
        Future.successful(Some(Json.parse(messages.toJson).as[MessageBlock]))
      case None =>
        Future.successful(None)
    }
  }



}


//Database Transfer Use
case class ChatRoom(override val typ: String = "chatRoom", members: Seq[User], roomId: String, messages: Seq[Message]) extends JsonRetrieve{
  require(typ == "chatRoom", "typ must be 'chatRoom'")
}
case class MessageBlock(messageId: UUID, roomId: String, timeStamp: Instant = Instant.now(), messages: Seq[Message])
