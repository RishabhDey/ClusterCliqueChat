package model
import scala.collection.concurrent.TrieMap



//Database Transfer Use
case class ChatRoom(override val typ: String = "chatRoom", members: Seq[User], roomId: String, messages: Seq[Message]) extends JsonRetrieve{
  require(typ == "chatRoom", "typ must be 'chatRoom'")
}




//This should call to database later
class ChatModel {
  private val allChats = TrieMap[String, ChatRoom]()
  def saveSnapshot(snapshot: ChatRoom): Unit = {
    allChats.update(snapshot.roomId, snapshot)
  }
}




