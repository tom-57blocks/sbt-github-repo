package xyz.jia.scala.commons.playutils.http.rest

import java.net.URL
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.ws.WSResponse

import xyz.jia.scala.commons.playutils.http.rest.RestCircuit.RoutedRequest
import xyz.jia.scala.commons.playutils.http.rest.RestCircuitManager._

class RestCircuitManager extends Actor {

  private def createCircuit(
      name: String,
      circuitConfig: RestCircuitConfig
  )(implicit ec: ExecutionContext): ActorRef =
    context.actorOf(RestCircuit.props(circuitConfig), name)

  private def findOrCreateCircuit(
      name: String,
      circuitConfig: RestCircuitConfig
  )(implicit ec: ExecutionContext): ActorRef =
    context.child(name).getOrElse(createCircuit(name, circuitConfig))

  def receive: Receive = {
    case UnRoutedRestRequest(url, requestSender, circuitConfig, ec, circuitName) =>
      findOrCreateCircuit(
        circuitName.getOrElse(new URL(url).getHost),
        circuitConfig
      )(ec).forward(RoutedRequest(requestSender))
  }

}

object RestCircuitManager {

  def defaultName = s"RestCircuitManager-${UUID.randomUUID().toString.replaceAll("-", "")}"

  case class UnRoutedRestRequest(
      url: String,
      requestSender: () => Future[WSResponse],
      circuitConfig: RestCircuitConfig,
      executionContext: ExecutionContext,
      circuitName: Option[String] = None
  )

  def props: Props = Props(new RestCircuitManager())

}
