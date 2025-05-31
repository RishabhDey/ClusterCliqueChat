package model
import java.time.Instant
import scala.collection.concurrent.TrieMap
import java.time.temporal.ChronoUnit



//Database Transfer Use
case class ChatRoom(override val typ: String = "chatRoom", members: Seq[User], roomId: String, messages: Seq[Message]) extends JsonRetrieve{
  require(typ == "chatRoom", "typ must be 'chatRoom'")
}




//This should call to database later
class ChatModel {
  //maps RoomId -> ChatRoom
  private val allChats = TrieMap[String, ChatRoom]()
  //maps UserId -> RefreshToken
  private val refreshTokens = TrieMap[String, RefreshToken]()
  def saveSnapshot(snapshot: ChatRoom): Unit = {
    allChats.update(snapshot.roomId, snapshot)
  }
  def getSnapshot(roomId: String): Option[ChatRoom] = {
    allChats.get(roomId)
  }
  def getRefreshToken(user: User): Option[RefreshToken] = {
    refreshTokens.get(user.userId).filter(token => token.user.userId == user.userId && token.expiry.isAfter(Instant.now))
  }
  def setRefreshToken(user: User): Option[RefreshToken] = {
    refreshTokens(user.userId) = AuthUtil.generateNewRefreshToken(user)
    refreshTokens.get(user.userId)
  }
  def getNewAccessToken(user: User, refreshToken: RefreshToken): Option[String] = {
    getRefreshToken(user).filter(_.equals(refreshToken)).map{_ =>
      AuthUtil.generateNewAccessToken(user)
    }
  }

}




