package xyz.jia.scala.commons.playutils.http.rest

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import akka.actor.{Actor, Props}
import akka.pattern.{CircuitBreaker, pipe}
import play.api.libs.ws.WSResponse

import xyz.jia.scala.commons.playutils.http.rest.RestCircuit._

class RestCircuit(circuitConfig: RestCircuitConfig)(implicit ec: ExecutionContext) extends Actor {

  val breaker = new CircuitBreaker(
    scheduler = context.system.scheduler,
    maxFailures = circuitConfig.maxFailures,
    callTimeout = circuitConfig.callTimeout,
    resetTimeout = circuitConfig.resetTimeout
  )

  def receive: Receive = { case RoutedRequest(requestSender) =>
    breaker.withCircuitBreaker(requestSender.apply(), failureDefinition).pipeTo(sender())
    ()
  }

  def failureDefinition: Try[WSResponse] => Boolean = {
    case Success(response) => !circuitConfig.isSupportedSuccessCode(response.status)
    case Failure(_)        => true
  }

}

object RestCircuit {

  case class RoutedRequest(requestSender: () => Future[WSResponse])

  def props(config: RestCircuitConfig)(implicit ec: ExecutionContext): Props =
    Props(new RestCircuit(config))

}
