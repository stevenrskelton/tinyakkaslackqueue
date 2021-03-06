package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.blocks._
import org.slf4j.event.Level

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CreateTaskModal(slackUser: SlackUser, slackTaskMeta: SlackTaskMeta, zonedDateTimeOpt: Option[ZonedDateTime])(implicit slackFactories: SlackFactories) extends SlackModal {

  private val submitButtonText = if (zonedDateTimeOpt.isEmpty) {
    if (slackFactories.isExecuting) "Queue" else "Run"
  } else {
    "Schedule"
  }

  private val dateTimeBlocks = zonedDateTimeOpt.fold("") {
    zonedDateTime =>
      s""",{
			"type": "input",
			"element": {
				"type": "datepicker",
				"initial_date": "${zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
				"placeholder": {
					"type": "plain_text",
					"text": "Select a date",
					"emoji": true
				},
				"action_id": "${ActionId.DataScheduleDate.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Start date",
				"emoji": true
			}
		},
		{
			"type": "input",
			"element": {
				"type": "timepicker",
				"initial_time": "${zonedDateTime.format(DateTimeFormatter.ofPattern("hh:mm"))}",
				"placeholder": {
					"type": "plain_text",
					"text": "Start Time",
					"emoji": true
				},
				"action_id": "${ActionId.DataScheduleTime.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Start time",
				"emoji": true
			}
		}"""
  }

  private val advancedOptions =
    s""",{
			"type": "divider"
		},
		{
			"type": "input",
			"element": {
				"type": "multi_users_select",
				"placeholder": {
					"type": "plain_text",
					"text": "Users",
					"emoji": true
				},
        "initial_users": ["${slackUser.id.value}"],
				"action_id": "${ActionId.DataNotifyOnComplete.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Users to notify on task complete",
				"emoji": true
			}
		},
		{
			"type": "input",
			"element": {
				"type": "multi_users_select",
				"placeholder": {
					"type": "plain_text",
					"text": "Users",
					"emoji": true
				},
        "initial_users": ["${slackUser.id.value}"],
				"action_id": "${ActionId.DataNotifyOnFailure.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Users to notify on task failure",
				"emoji": true
			}
		}"""

  private def logLevelBlock(level: Level): String =
    s"""{
    "text": {
      "type": "plain_text",
      "text": "${logLevelEmoji(level)} ${level.name}",
      "emoji": true
    },
    "value": "${level.name}"
  }"""

  override def toString: String =
    s"""{
  ${PrivateMetadata(slackTaskMeta.index.toString).block},
	"title": {
		"type": "plain_text",
		"text": "$AppModalTitle",
		"emoji": true
	},
	"submit": {
		"type": "plain_text",
		"text": "$submitButtonText",
		"emoji": true
	},
	"type": "modal",
  ${CallbackId.Create.block},
	"close": {
		"type": "plain_text",
		"text": "Cancel",
		"emoji": true
	},
	"blocks": [
     {
      "type": "header",
      "text": {
        "type": "plain_text",
        "text": "${slackTaskMeta.factory.name.getText}",
        "emoji": true
      }
    },{
			"type": "context",
			"elements": [
				{
					"type": "mrkdwn",
					"text": "${slackTaskMeta.factory.description.getText}"
				}
			]
		}
		$dateTimeBlocks
    $advancedOptions
		,{
			"type": "divider"
		},
		{
			"type": "input",
			"element": {
				"type": "static_select",
				"placeholder": {
					"type": "plain_text",
					"text": "Select an item",
					"emoji": true
				},
				"options": [${Seq(Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG).map(logLevelBlock).mkString(",")}],
        "initial_option": ${logLevelBlock(Level.WARN)},
				"action_id": "${ActionId.DataLogLevel.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Log Level",
				"emoji": true
			}
    }
 	]
}"""

}
