package xyz.jia.scala.commons.playutils.http.rest

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}

import akka.stream.Materializer
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}

import xyz.jia.scala.commons.playutils.json.JsonMaskConfig

@Singleton
class RestClient @Inject() (ws: WSClient)(implicit mat: Materializer) {

  private def prepareRequest(
      url: String,
      headers: Map[String, String],
      queryParams: Map[String, String],
      timeout: Duration,
      auth: Option[AuthConfig],
      logRequest: Boolean,
      hasSensitivePayload: Boolean
  )(implicit ec: ExecutionContext, maskConfig: JsonMaskConfig): WSRequest = {
    var request = ws
      .url(url)
      .addHttpHeaders(headers.toSeq: _*)
      .addQueryStringParameters(queryParams.toSeq: _*)
      .withRequestTimeout(timeout)

    request =
      if (logRequest) request.withRequestFilter(new LoggingFilter(hasSensitivePayload)) else request

    auth
      .map(config => request.withAuth(config.username, config.password, config.scheme))
      .getOrElse(request)
  }

  def get(
      url: String,
      headers: Map[String, String] = Map.empty,
      queryParams: Map[String, String] = Map.empty,
      timeout: Duration = 10.seconds,
      auth: Option[AuthConfig] = None,
      logRequest: Boolean = true,
      hasSensitivePayload: Boolean = true
  )(implicit ec: ExecutionContext, maskConfig: JsonMaskConfig): Future[WSResponse] =
    prepareRequest(url, headers, queryParams, timeout, auth, logRequest, hasSensitivePayload)
      .get()

  def post[T: BodyWritable](
      url: String,
      body: T,
      headers: Map[String, String] = Map.empty,
      queryParams: Map[String, String] = Map.empty,
      timeout: Duration = 10.seconds,
      auth: Option[AuthConfig] = None,
      logRequest: Boolean = true,
      hasSensitivePayload: Boolean = true
  )(implicit ec: ExecutionContext, maskConfig: JsonMaskConfig): Future[WSResponse] =
    prepareRequest(url, headers, queryParams, timeout, auth, logRequest, hasSensitivePayload)
      .post(body)

}
