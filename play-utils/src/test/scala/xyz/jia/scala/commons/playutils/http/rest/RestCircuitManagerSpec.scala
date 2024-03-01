package xyz.jia.scala.commons.playutils.http.rest

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

import xyz.jia.scala.commons.playutils.http.rest.RestCircuitManager.UnRoutedRestRequest

class RestCircuitManagerSpec
    extends TestKit(ActorSystem("RestCircuitManagerSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures
    with MockitoSugar {

  implicit val patience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  var restCircuitManager: ActorRef = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    restCircuitManager = system.actorOf(RestCircuitManager.props, RestCircuitManager.defaultName)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  "RestCircuitManager" should {
    val url = "http://localhost"
    val circuitConfig: RestCircuitConfig =
      RestCircuitConfig(5, 10.seconds, 1.minute, Set(200))

    "correctly send an UnRoutedRestRequest to a RestCircuit" in {
      val mockResponse = mock[WSResponse]
      when(mockResponse.status).thenReturn(200)
      val requestSender = () => Future.successful(mockResponse)
      val unRoutedRequest =
        UnRoutedRestRequest(url, requestSender, circuitConfig, ExecutionContext.global)

      restCircuitManager ! unRoutedRequest
      expectMsg(mockResponse)
    }

    "ignore non UnRoutedRestRequest message" in {
      restCircuitManager ! Json.obj("key" -> "value")
      expectNoMessage()
    }
  }

}
