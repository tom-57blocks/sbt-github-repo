package xyz.jia.scala.commons.playutils.http.rest

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.libs.ws._

import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.playutils.logging.NamedLogger

class LoggingFilter(
    val hasSensitivePayload: Boolean = true
)(implicit ec: ExecutionContext, mat: Materializer, maskConfig: JsonMaskConfig)
    extends WSRequestFilter
    with NamedLogger {

  override def apply(executor: WSRequestExecutor): WSRequestExecutor =
    (request: StandaloneWSRequest) => {
      extractRequestDetails(request).flatMap { requestDetails =>
        val marker = rawJsonAppender("requestDetails", requestDetails)
        logger.info("Outgoing HTTP request")(marker)
        executor(request).flatMap(extractResponseDetails).map { case (response, responseDetails) =>
          val marker = rawJsonAppender("responseDetails", responseDetails)
          logger.info("Incoming HTTP response")(marker)
          response
        }
      }
    }

  def extractRequestDetails(request: StandaloneWSRequest): Future[JsObject] = {
    val requestDetails = Json.obj(
      "url" -> request.url,
      "headers" -> request.headers,
      "queryString" -> request.queryString
    )
    request.body match {
      case InMemoryBody(bytes) =>
        Future.successful(parseBody(requestDetails, bytes))
      case SourceBody(source) =>
        source
          .runWith(Sink.fold[ByteString, ByteString](ByteString.empty)(_.concat(_)))
          .map(parseBody(requestDetails, _))
      case _ => Future.successful(requestDetails)
    }
  }

  def extractResponseDetails(
      response: StandaloneWSResponse
  ): Future[(StandaloneWSResponse, JsObject)] = {
    val responseDetails = Json.obj(
      "url" -> response.uri,
      "headers" -> response.headers
    )
    response.bodyAsSource
      .runWith(Sink.fold[ByteString, ByteString](ByteString.empty)(_.concat(_)))
      .map(parseBody(responseDetails, _))
      .map((response, _))
  }

  def parseBody(details: JsObject, bytes: ByteString): JsObject =
    details + ("body" -> Try(Json.parse(bytes.toArray)).getOrElse {
      if (hasSensitivePayload) {
        if (bytes.isEmpty) JsString("") else JsString("Body cannot be parsed as JSON for masking")
      } else {
        JsString(bytes.utf8String)
      }
    })

}
