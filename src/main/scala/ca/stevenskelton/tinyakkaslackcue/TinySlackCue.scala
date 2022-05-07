package ca.stevenskelton.tinyakkaslackcue

import akka.actor.ActorSystem
import akka.stream.Materializer
import ca.stevenskelton.tinyakkaslackcue.blocks.SlackTaskThread
import com.typesafe.config.Config
import org.slf4j.Logger

import java.time.ZonedDateTime
import java.util.UUID

class TinySlackCue(slackClient: SlackClient, logger: Logger)(implicit actorSystem: ActorSystem, config: Config, materializer: Materializer) {
  private val interactiveTimer = new InteractiveJavaUtilTimer[SlackTask](logger)

  def listScheduledTasks: Seq[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask] = interactiveTimer.list

  def cancelScheduledTask(uuid: UUID): Boolean = interactiveTimer.cancel(uuid)

  def scheduleSlackTask(name: String, time: Option[ZonedDateTime], slackTaskFactories: SlackTaskFactories): Option[SlackTask] = {
    slackTaskFactories.factories.find(_.name == name).map {
      factory =>
        val slackPlaceholder = slackClient.chatPostMessage(SlackTaskThread.placeholderThread(name))
        implicit val sc = slackClient
        val slackTask = factory.create(
          ts = SlackTs(slackPlaceholder),
          createdBy = SlackUserId.Empty,
          notifyOnError = Nil,
          notifyOnComplete = Nil
        )
        val scheduledTask = time.fold(interactiveTimer.schedule(slackTask))(interactiveTimer.schedule(slackTask, _))
        slackClient.chatUpdateBlocks(SlackTaskThread.schedule(scheduledTask), slackTask.ts)
        slackClient.pinsAdd(slackTask.ts)
        Some(slackTask)
    }.getOrElse {
      None
    }

  }

}