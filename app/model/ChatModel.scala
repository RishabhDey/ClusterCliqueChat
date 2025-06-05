package model
import java.time.Instant
import scala.collection.concurrent.TrieMap
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._

//This should call to database later
class ChatModel {

  val mongoClient = MongoClient(APIKeys.MongoKey)

  //These classes are temporary, everything here will eventually be in the Database.
  //maps RoomId -> ChatRoom
  private val allChats = TrieMap[String, ChatRoom]()
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

}


//Database Transfer Use
case class ChatRoom(override val typ: String = "chatRoom", members: Seq[User], roomId: String, messages: Seq[Message]) extends JsonRetrieve{
  require(typ == "chatRoom", "typ must be 'chatRoom'")
}


