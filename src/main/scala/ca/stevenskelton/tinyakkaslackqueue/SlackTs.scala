package ca.stevenskelton.tinyakkaslackqueue

import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.Message
import play.api.libs.json.{JsString, JsValue, Writes}

trait SlackTs {
  def value: String

  override def toString: String = value
}

trait SlackThreadTs extends SlackTs

case class SlackHistoryThreadTs(value: String) extends SlackThreadTs

object SlackHistoryThreadTs {
  //  def apply(chatPostMessageResponse: ChatPostMessageResponse): SlackHistoryThreadTs = SlackHistoryThreadTs(chatPostMessageResponse.getTs)
  def apply(message: Message): SlackHistoryThreadTs = SlackHistoryThreadTs(message.getTs)
}


case class SlackTaskThreadTs(value: String) extends SlackThreadTs

object SlackTaskThreadTs {
  def apply(chatPostMessageResponse: ChatPostMessageResponse): SlackTaskThreadTs = SlackTaskThreadTs(chatPostMessageResponse.getTs)
}

object SlackTs {
  //  val Empty = SlackTs("")
  //
  //  def apply(chatPostMessageResponse: ChatPostMessageResponse): SlackTs = SlackTs(chatPostMessageResponse.getTs)
  //
  //  def apply(message: Message): SlackTs = SlackTs(message.getTs)
  //
  //  implicit val reads = implicitly[Reads[String]].map(SlackTs(_))
  implicit val writes = new Writes[SlackTs] {
    override def writes(o: SlackTs): JsValue = JsString(o.value)
  }
}
