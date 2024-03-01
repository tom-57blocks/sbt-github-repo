package xyz.jia.scala.commons.playutils.tasks

import scala.concurrent.ExecutionContext

import net.logstash.logback.marker.LogstashMarker
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.mvc.{Action, BaseController, ControllerComponents, Request}

import xyz.jia.scala.commons.playutils.http.ControllerUtils
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.playutils.logging.NamedLogger
import xyz.jia.scala.commons.utils.ErrorMessage

abstract class HttpInvocableTaskController(
    val httpInvocableTaskService: HttpInvocableTaskService,
    override val controllerComponents: ControllerComponents,
    implicit val jsonMaskConfig: JsonMaskConfig,
    implicit val ec: ExecutionContext
) extends BaseController
    with NamedLogger
    with ControllerUtils {

  def executeTask: Action[JsValue] =
    Action.async(parse.tolerantJson) { request: Request[JsValue] =>
      implicit val correlationId: String = getRequestId(request)
      logger.info("Received task invocation request")(httpRequestAppender(request))
      withJsonPayloadValidation[HttpInvocableTaskRequest](request) { invocableTaskRequest =>
        httpInvocableTaskService.processTask(invocableTaskRequest).map {
          case Right(_) =>
            generateResponse(request, "2000", ACCEPTED)
          case Left((errorMessages: Seq[ErrorMessage], errorCode)) =>
            val errors = Some(JsArray(errorMessages.map(msg => JsString(msg.message))))
            val marker = rawJsonAppender("validationErrors", Json.toJson(errors))
              .and[LogstashMarker](correlationIdAppender)
            logger.info("Task invocation request invalid")(marker)
            generateResponse(request, errorCode.code, BAD_REQUEST, errors = errors)
        }
      }
    }

}
