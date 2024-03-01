package xyz.jia.scala.commons.playutils.http

import java.util.UUID

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.Try

import play.api.Logging
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json._
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Results.Status
import play.api.mvc.{Request, RequestHeader, Result}

import xyz.jia.scala.commons.playutils.ErrorMessageHelpers.JsErrorAsErrorMessage
import xyz.jia.scala.commons.playutils.http.ControllerUtils.Attrs

trait ControllerUtils extends Logging {

  /** Generates a valid request ID */
  def generateRequestId: String = UUID.randomUUID().toString

  /** Extracts a request ID from a request. If no request ID exists a new one is generated.
    *
    * @param request
    *   The request from which to extract the request ID
    * @tparam T
    *   Subclass of `RequestHeader`
    * @return
    *   the request ID
    */
  def getRequestId[T <: RequestHeader](request: T): String =
    request.attrs
      .get(Attrs.requestId)
      .orElse(request.headers.toSimpleMap.get(ControllerUtils.requestIdHeaderName))
      .getOrElse(generateRequestId)

  /** Calculates the execution time for a request.
    *
    * @param request
    *   The request whose execution time is to be computed
    * @param startTime
    *   Optional time the request is known to have started. If not supplied, the it will be looked
    *   up from request's `requestProcessingStartTime` attribute. This attribute is auto set if the
    *   filter `xyz.jia.scala.commons.playutils.http.CommonAttributesInclusionFilter` is enabled —
    *   it is enabled by default.
    *
    * @tparam T
    *   Subclass of `RequestHeader`
    * @return
    *   The request execution time in milliseconds otherwise None if the startTime can't be
    *   determined
    */
  def getExecutionTime[T <: RequestHeader](
      request: T,
      startTime: Option[FiniteDuration] = None
  ): Option[Long] =
    startTime
      .orElse(request.attrs.get(Attrs.processingStartTime))
      .map((start: FiniteDuration) => systemNanoTime.toNanos - start.toNanos)
      .map(_.nanos.toMillis)

  def generateResponse[T <: RequestHeader](
      request: T,
      resultCode: String,
      httpStatusCode: Int,
      data: JsValue = JsNull,
      errors: Option[JsArray] = None,
      startTime: Option[FiniteDuration] = None,
      requestId: Option[String] = None
  ): Result = {
    // We should ideally throw an exception when we cannot obtain the processing start time however
    // from testing, Play! is losing request attributes and headers when internal server errors occur
    // We are returning -1 to be explicit that the processing time shouldn't be trusted.
    val executionTime: Long =
      Try(getExecutionTime(request, startTime)).toOption.flatten.getOrElse(-1)

    val response = ServiceResponsePayload(
      data = data,
      meta = ServiceResponsePayload.Metadata(
        errors = errors,
        executionTime = executionTime,
        requestId = requestId.getOrElse(getRequestId(request)),
        resultCode = resultCode
      )
    )
    Status(httpStatusCode)(Json.toJson(response))
  }

  /** Validates a `JSON` request payload and returns a `400` HTTP status code if the payload is
    * invalid otherwise, provides the valid payload to the specified `validPayload` handler
    * @param request
    *   The HTTP request
    * @param clientErrorCode
    *   The service specific error code to return for invalid payloads. Defaults to `4000`
    * @param validPayloadHandler
    *   The handler for processing valid payloads
    * @param tReads
    *   The `JSON` deserializer to use to convert the `JSON` to type `T`
    * @tparam T
    *   The type to deserialize the `JSON` to
    * @return
    *   `Result`
    */
  def withJsonPayloadValidation[T](
      request: Request[JsValue],
      clientErrorCode: String = "4000"
  )(validPayloadHandler: T => Future[Result])(implicit tReads: Reads[T]): Future[Result] = {
    request.body.validate[T] match {
      case JsSuccess(validPayload, _) =>
        validPayloadHandler(validPayload)
      case JsError(errors) =>
        logger.warn(s"Invalid payload provided. Errors => ${JsError.toJson(errors)}")
        val errorsMessages = JsError(errors).errorMessages.map(error => JsString(error.message))
        Future.successful(
          generateResponse(
            request,
            clientErrorCode,
            BAD_REQUEST,
            errors = Some(JsArray(errorsMessages))
          )
        )
    }
  }

  // $COVERAGE-OFF$

  /** Exposed to help in testing */
  def systemNanoTime: FiniteDuration = System.nanoTime().nanos
  // $COVERAGE-ON$

}

object ControllerUtils extends ControllerUtils {

  val requestIdHeaderName: String = "X-Request-ID"

  object Attrs {

    val requestId: TypedKey[String] = TypedKey(requestIdHeaderName)

    val processingStartTime: TypedKey[FiniteDuration] = TypedKey("requestProcessingStartTime")

  }

}
