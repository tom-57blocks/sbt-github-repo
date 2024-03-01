package xyz.jia.scala.commons.playutils.http.rest

import javax.inject.{Inject, Singleton}

import scala.concurrent.duration.{DurationInt, DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.ws.{BodyWritable, WSResponse}

import xyz.jia.scala.commons.playutils.http.rest.RestCircuitManager._
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig

@Singleton
class CircuitedRestClient @Inject() (restClient: RestClient)(implicit actorSystem: ActorSystem) {

  lazy val requestManager: ActorRef =
    actorSystem.actorOf(RestCircuitManager.props, RestCircuitManager.defaultName)

  def get(
      url: String,
      headers: Map[String, String] = Map.empty,
      queryParams: Map[String, String] = Map.empty,
      timeout: FiniteDuration = 10.seconds,
      auth: Option[AuthConfig] = None,
      logRequest: Boolean = true,
      hasSensitivePayload: Boolean = true,
      circuitName: Option[String] = None
  )(implicit
      ec: ExecutionContext,
      maskConfig: JsonMaskConfig,
      circuitConfig: RestCircuitConfig
  ): Future[WSResponse] = {
    executeRequest(
      url = url,
      requestSender = () => {
        restClient.get(
          url, headers, queryParams, timeout, auth, logRequest, hasSensitivePayload
        )
      },
      circuitName = circuitName
    )
  }

  def post[T: BodyWritable](
      url: String,
      body: T,
      headers: Map[String, String] = Map.empty,
      queryParams: Map[String, String] = Map.empty,
      timeout: FiniteDuration = 10.seconds,
      auth: Option[AuthConfig] = None,
      logRequest: Boolean = true,
      hasSensitivePayload: Boolean = true,
      circuitName: Option[String] = None
  )(implicit
      ec: ExecutionContext,
      maskConfig: JsonMaskConfig,
      circuitConfig: RestCircuitConfig
  ): Future[WSResponse] = {
    executeRequest(
      url = url,
      requestSender = () => {
        restClient.post(
          url, body, headers, queryParams, timeout, auth, logRequest, hasSensitivePayload
        )
      },
      circuitName = circuitName
    )
  }

  def executeRequest(
      url: String,
      requestSender: () => Future[WSResponse],
      circuitName: Option[String] = None
  )(implicit ec: ExecutionContext, circuitConfig: RestCircuitConfig): Future[WSResponse] = {
    // Double the request timeout so that the circuit doesn't timeout before the request does
    implicit val askTimeout: Timeout = (circuitConfig.callTimeout.toNanos * 2).nanos
    requestManager
      .ask(UnRoutedRestRequest(url, requestSender, circuitConfig, ec, circuitName))
      .mapTo[WSResponse]
      .map { response =>
        if (circuitConfig.isSupportedSuccessCode(response.status)) {
          response
        } else {
          val message = s"Unsupported response status returned. status => '${response.status}'"
          throw new UnsupportedApiResponseException(message, response)
        }
      }
  }

}
