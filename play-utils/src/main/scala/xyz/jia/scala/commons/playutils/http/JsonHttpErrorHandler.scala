package xyz.jia.scala.commons.playutils.http

import java.util.UUID

import scala.concurrent.Future

import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsArray, JsString}
import play.api.mvc.{RequestHeader, Result}

/** An error handler that generates JSON formatted responses for client and server side errors in
  * controllers.
  */
class JsonHttpErrorHandler extends HttpErrorHandler with ControllerUtils with Logging {

  val clientError = "4000"

  val serverError = "5000"

  override def onClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] =
    Future.successful(
      generateResponse(
        request = request,
        resultCode = clientError,
        httpStatusCode = statusCode,
        errors = Some(JsArray(Seq(JsString(message))))
      )
    )

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val logMessage = s"An internal server error occurred errorId -> $generateErrorId"
    logger.error(logMessage, exception)

    Future.successful(
      generateResponse(
        request = request,
        resultCode = serverError,
        httpStatusCode = INTERNAL_SERVER_ERROR,
        errors = Some(JsArray(Seq(JsString(logMessage))))
      )
    )
  }

  def generateErrorId: String = UUID.randomUUID().toString.replaceAll("-", "").take(8)

}
