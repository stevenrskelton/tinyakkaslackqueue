package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.util.DateUtils
import ca.stevenskelton.tinyakkaslackcue.{SlackBlocksAsString, SlackTs, SlackUserId}

import java.time.{Duration, ZonedDateTime}

object TaskHistoryItem {
  implicit val ordering = new Ordering[TaskHistoryItem] {
    override def compare(x: TaskHistoryItem, y: TaskHistoryItem): Int = x.date.compareTo(y.date)
  }
}

case class TaskHistoryItem(slackTs: SlackTs, date: ZonedDateTime, duration: Duration, createdBy: SlackUserId, isSuccess: Boolean) {

  def toBlocks: SlackBlocksAsString = {
    val blocksAsString = if (isSuccess) {
      s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":white_check_mark: *Last Success:* ${DateUtils.humanReadable(date)}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Logs",
      "emoji": true
    },
    "value": "click_me_123",
    "action_id": "button-action"
  }
}"""
    } else {
      s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":no_entry_sign: *Last Failure:* ${DateUtils.humanReadable(date)}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Logs",
      "emoji": true
    },
    "value": "click_me_123",
    "action_id": "button-action"
  }
}"""
    }
    SlackBlocksAsString(blocksAsString)
  }
}

