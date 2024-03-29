//package ca.stevenskelton.tinyakkaslackqueue.api
//
//import org.apache.pekko.http.scaladsl.coding.Coders
//import org.apache.pekko.http.scaladsl.model.StatusCodes.OK
//import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
//import org.apache.pekko.http.scaladsl.server.Directives.*
//import org.apache.pekko.http.scaladsl.server.{Directives, Route}
//import org.apache.pekko.http.scaladsl.unmarshalling.FromRequestUnmarshaller
//import org.apache.pekko.stream.Materializer
//import org.apache.pekko.stream.scaladsl.Sink
//import org.apache.pekko.util.ByteString
//import ca.stevenskelton.tinyakkaslackqueue.*
//import ca.stevenskelton.tinyakkaslackqueue.blocks.*
//import ca.stevenskelton.tinyakkaslackqueue.views.*
//import com.typesafe.config.Config
//import org.slf4j.Logger
//import play.api.libs.json.{JsObject, Json}
//
//import java.time.ZonedDateTime
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.{Failure, Success, Try}
//
//class SlackRoutes(slackTaskFactories: SlackTaskFactories, slackClient: SlackClient, config: Config)(implicit logger: Logger, materializer: Materializer) {
//
//  implicit lazy val slackFactories: SlackFactories = {
//    SlackFactories.initialize(slackTaskFactories, config)(logger, slackClient, materializer)
//  }
//
//  private val unmarshaller = new FromRequestUnmarshaller[(String, JsObject)] {
//    override def apply(value: HttpRequest)(implicit ec: ExecutionContext, materializer: Materializer): Future[(String, JsObject)] = {
//      value.entity.getDataBytes.runWith(Sink.fold[ByteString, ByteString](ByteString.empty)(_ ++ _), materializer).map {
//        byteString =>
//          val body = byteString.utf8String
//          logger.debug(s"SE:\n```$body```")
//          val jsObject = Json.parse(byteString.utf8String).as[JsObject]
//          ((jsObject \ "type").as[String], jsObject)
//      }
//    }
//  }
//
//  private def publishHomeTab(slackUserId: SlackUserId, slackHomeTab: SlackHomeTab)(implicit logger: Logger, slackFactories: SlackFactories): Future[Unit] = {
//    slackFactories.slackClient.viewsPublish(slackUserId, slackHomeTab).fold(ex => Future.failed(ex), {
//      response =>
//        if (response.isOk) {
//          logger.debug(s"Created home view for ${slackUserId.value}")
//          Future.successful(())
//        } else {
//          logger.error(s"Home view creation failed: ${response.getError}")
//          logger.error(s"\n```${slackHomeTab.toString}```\n")
//          Future.failed(new Exception(response.getError))
//        }
//    })
//  }
//
//  private def cancelTask(ts: SlackTs, slackPayload: SlackPayload)(implicit slackFactories: SlackFactories, logger: Logger): Try[HomeTab] = {
//    slackFactories.cancelScheduledTask(ts).map {
//      cancelledTask =>
//        logger.error("Cancelled Task")
//        val view = new CancelTaskModal(cancelledTask, slackPayload)
//        slackFactories.slackClient.viewsUpdate(slackPayload.viewId, view).foreach {
//          update =>
//            if (!update.isOk) {
//              logger.error(view.toString)
//              logger.error(update.getError)
//            }
//        }
//        val zoneId = slackFactories.slackClient.userZonedId(slackPayload.user.id)
//        Success(new HomeTab(zoneId))
//    }.getOrElse {
//      val ex = new Exception(s"Could not find task ts ${ts.value}")
//      logger.error("handleSubmission", ex)
//      Failure(ex)
//    }
//  }
//
//  val slackEventRoute: Route = Directives.post {
//    entity(as[(String, JsObject)](unmarshaller)) {
//      case ("url_verification", jsObject) => complete((jsObject \ "challenge").as[String])
//      case ("event_callback", jsObject) =>
//        val eventObject = (jsObject \ "event").as[JsObject]
//        logger.info(s"EventCallback\n```${Json.stringify(eventObject)}```")
//        val flow = (eventObject \ "type").as[String] match {
//          case "app_home_opened" =>
//            val slackUserId = SlackUserId((eventObject \ "user").as[String])
//            val zoneId = slackFactories.slackClient.userZonedId(slackUserId)
//            publishHomeTab(slackUserId, new HomeTab(zoneId))
//          case unknown => throw new NotImplementedError(s"Slack event $unknown not implemented: ${Json.stringify(jsObject)}")
//        }
//        extractExecutionContext {
//          ec => complete(flow.map(_ => HttpResponse(OK))(ec))
//        }
//      case (t, jsObject) =>
//        throw new NotImplementedError(s"Slack message type $t not implemented: ${Json.stringify(jsObject)}")
//    }
//  }
//
//  val slackActionRoute: Route = Directives.post {
//    formField("payload") { payload =>
//      logger.info(s"Action payload:\n```$payload```\n")
//      val jsObject = Json.parse(payload).as[JsObject]
//      val slackPayload = SlackPayload(jsObject)
//
//      lazy val zoneId = slackFactories.slackClient.userZonedId(slackPayload.user.id)
//
//      val handler: Try[SlackView] = slackPayload.payloadType match {
//        case SlackPayload.BlockActions =>
//          val action = slackPayload.action
//          action match {
//            case SlackAction(ActionId.HomeTabRefresh, _) => Success(new HomeTab(zoneId))
//            case SlackAction(ActionId.TaskCancel, ButtonState(value)) => cancelTask(SlackTs(value), slackPayload)
//            case SlackAction(ActionId.HomeTabTaskHistory, ButtonState(value)) =>
//              Try {
//                val slackTaskInitialized = slackFactories.slackTasks.drop(value.toInt).head
//                new HomeTabTaskHistory(zoneId, slackTaskInitialized.slackTaskMeta.get.history(Nil))
//              }
//            case SlackAction(ActionId.ModalTaskQueue, ButtonState(value)) =>
//              Try {
//                val slackTaskInitialized = slackFactories.slackTasks.drop(value.toInt).head
//                new CreateTaskModal(slackPayload, slackTaskInitialized.slackTaskMeta.get, None)
//              }
//            case SlackAction(ActionId.ModalTaskSchedule, ButtonState(value)) =>
//              Try {
//                val slackTaskInitialized = slackFactories.slackTasks.drop(value.toInt).head
//                new CreateTaskModal(slackPayload, slackTaskInitialized.slackTaskMeta.get, Some(ZonedDateTime.now(zoneId)))
//              }
//            case SlackAction(ActionId.ModalQueuedTaskView, ButtonState(value)) =>
//              val ts = SlackTs(value)
//              val list = slackFactories.listScheduledTasks
//              val index = list.indexWhere(_.id == ts)
//              if (index == -1) {
//                val ex = new Exception(s"Task ts $ts not found")
//                logger.error("handleAction", ex)
//                Failure(ex)
//              } else {
//                Success(new ViewTaskModal(zoneId, list, index))
//              }
//            case SlackAction(ActionId.HomeTabConfiguration, _) => Success(new HomeTabConfigure(zoneId))
//            case SlackAction(ActionId.RedirectToTaskThread, _) => Success(SlackOkResponse)
//            case SlackAction(actionId, ChannelsState(value)) if slackPayload.callbackId.contains(CallbackId.HomeTabConfigure) =>
//              actionId.getIndex.foreach {
//                case (_, taskIndex) => slackFactories.updateFactoryLogChannel(taskIndex, TaskLogChannel(value.id))
//              }
//              //TODO: pass error
//              Success(new HomeTab(zoneId))
//          }
//        case SlackPayload.ViewSubmission if slackPayload.actionStates.contains(ActionId.TaskCancel) =>
//          val slackTs = SlackTs(slackPayload.actionStates(ActionId.TaskCancel).asInstanceOf[ButtonState].value)
//          slackFactories.cancelScheduledTask(slackTs).map {
//            _ => Success(new HomeTab(zoneId))
//          }.getOrElse {
//            val ex = new Exception(s"Could not find task ts ${slackTs.value}")
//            logger.error("handleSubmission", ex)
//            Failure(ex)
//          }
//        case SlackPayload.ViewSubmission if slackPayload.callbackId.contains(CallbackId.Create) =>
//          Try {
//            val slackTaskMeta = slackFactories.slackTasks.drop(slackPayload.privateMetadata.get.value.toInt).head.slackTaskMeta.get
//            val scheduledSlackTask = slackFactories.scheduleSlackTask(slackTaskMeta, slackPayload)
//            new HomeTab(zoneId)
//          }
//        case x =>
//          val ex = new NotImplementedError(s"Slack type $x, for:\n```$payload```")
//          logger.error("slackActionRoute", ex)
//          Failure(ex)
//      }
//      val view = handler match {
//        case Success(SlackOkResponse) => Future.successful(())
//        case Success(homeTab: SlackHomeTab) => publishHomeTab(slackPayload.user.id, homeTab)
//        case Success(slackModal: SlackModal) =>
//          slackFactories.slackClient.viewsOpen(slackPayload.triggerId, slackModal).foreach {
//            result =>
//              if (!result.isOk) {
//                if (result.getError == "missing_scope") {
//                  logger.error(s"Missing permission scope: ${result.getNeeded}")
//                } else {
//                  logger.debug(s"\n```$slackModal```\n")
//                  logger.error(result.getError)
//                }
//              }
//          }
//          Future.successful(())
//        case Failure(ex) => Future.failed(ex)
//      }
//      complete {
//        import scala.concurrent.ExecutionContext.Implicits.global
//        view.map {
//          _ => HttpResponse(OK)
//        }
//      }
//    }
//  }
//
//  val PublicRoutes: Route = {
//    Try(slackFactories)
//    encodeResponseWith(Coders.Gzip) {
//      concat(
//        path("slack" / "event")(slackEventRoute),
//        path("slack" / "action")(slackActionRoute)
//      )
//    }
//  }
//
//}
