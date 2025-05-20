package model
import scala.collection.concurrent.TrieMap



//Database Transfer Use
case class ChatRoom(members: Seq[User], roomId: String, messages: Seq[Message])

case class UserClique(user: User, cliques: Seq[String])


//This should call to database later
class ChatModel {
  private val allChats = TrieMap[String, ChatRoom]()
  def saveSnapshot(snapshot: ChatRoom): Unit = {
    allChats.update(snapshot.roomId, snapshot)
  }
}




