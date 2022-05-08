package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.{SlackBlocksAsString, SlackUser}
import org.slf4j.event.Level
import play.api.libs.json.JsObject

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ScheduleActionModal {

  val ActionIdScheduleDate = ActionId("datepicker-action")
  val ActionIdScheduleTime = ActionId("timepicker-action")
  val ActionIdNotifyOnComplete = ActionId("multi_users_select-action1")
  val ActionIdNotifyOnFailure = ActionId("multi_users_select-action2")
  val ActionIdLogLevel = ActionId("static_select-action")

  //https://api.slack.com/reference/surfaces/views
  def modal(slackUser: SlackUser,name: String, zonedDateTimeOpt: Option[ZonedDateTime], privateMetadata: PrivateMetadata): SlackBlocksAsString = {
    //mrkdwn

    val headerText = if(zonedDateTimeOpt.isEmpty){
      "Queue this task immediately."
    }else{
      "Set later date/time to schedule."
    }

    val dateTimeBlocks = zonedDateTimeOpt.fold("") {
      zonedDateTime =>
       s"""{
			"type": "input",
			"element": {
				"type": "datepicker",
				"initial_date": "${zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
				"placeholder": {
					"type": "plain_text",
					"text": "Select a date",
					"emoji": true
				},
				"action_id": "${ActionIdScheduleDate.value}"
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
				"action_id": "${ActionIdScheduleTime.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Start time",
				"emoji": true
			}
		},"""
    }

    SlackBlocksAsString(
      s"""{
  "private_metadata": "${privateMetadata.value}",
	"title": {
		"type": "plain_text",
		"text": "New $name Task",
		"emoji": true
	},
	"submit": {
		"type": "plain_text",
		"text": "Schedule",
		"emoji": true
	},
	"type": "modal",
	"close": {
		"type": "plain_text",
		"text": "Cancel",
		"emoji": true
	},
	"blocks": [
		{
			"type": "section",
			"text": {
				"type": "plain_text",
				"text": "$headerText"
			}
		},
		$dateTimeBlocks
		{
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
				"action_id": "${ActionIdNotifyOnComplete.value}",
        "initial_value": "${slackUser.username}"
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
				"action_id": "${ActionIdNotifyOnFailure.value}",
        "initial_value": "${slackUser.username}"
			},
			"label": {
				"type": "plain_text",
				"text": "Users to notify on task failure",
				"emoji": true
			}
		},
		{
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
				"options": [${
        Seq(Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG).map {
          level =>
            s"""{
						"text": {
							"type": "plain_text",
							"text": "${logLevelEmoji(level)} ${level.name}",
							"emoji": true
						},
						"value": "${level.name}"
					}"""
        }.mkString(",")
      }],
				"action_id": "${ActionIdLogLevel.value}",
        "initial_value": "${Level.INFO.name}"
			},
			"label": {
				"type": "plain_text",
				"text": "Log Level",
				"emoji": true
			}
		}
	]
}""")
  }

  def parseViewSubmission(jsObject: JsObject): (PrivateMetadata, Map[ActionId, State]) = {
    val privateMetadata = PrivateMetadata((jsObject \ "private_metadata").asOpt[String].getOrElse(""))
    val actionStates = State.parseActionStates(jsObject \ "view" \ "state" \ "values")
    (privateMetadata, actionStates)
  }

}
