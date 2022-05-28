package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue.TestData
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsDefined, JsObject, JsString, Json}

class SlackViewSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures {

  "queue" should {
    "read fields" in {
      val view = SlackView.createHomeTab(TestData.slackTaskFactories.history)
      val json = Json.parse(view.toString).as[JsObject]
      json \ "type" shouldBe JsDefined(JsString("home"))
    }
  }
}