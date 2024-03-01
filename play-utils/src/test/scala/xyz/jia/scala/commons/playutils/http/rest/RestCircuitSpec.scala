package xyz.jia.scala.commons.playutils.http.rest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.CircuitBreakerOpenException
import akka.testkit.{ImplicitSender, TestKit}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

import xyz.jia.scala.commons.playutils.http.rest.RestCircuit.RoutedRequest

class RestCircuitSpec
    extends TestKit(ActorSystem("RestCircuitSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures
    with MockitoSugar
    with Matchers {

  implicit val patience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val circuitConfig: RestCircuitConfig =
    RestCircuitConfig(1, 10.seconds, 1.minute, Set(200))

  var restCircuit: ActorRef = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    restCircuit = system.actorOf(RestCircuit.props(circuitConfig))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  "RestCircuit" should {
    "correctly execute request sender and return response" in {
      val mockResponse = mock[WSResponse]
      when(mockResponse.status).thenReturn(200)
      val requestSender = () => Future.successful(mockResponse)
      val routedRequest = RoutedRequest(requestSender)

      restCircuit ! routedRequest
      expectMsg(mockResponse)
    }

    "correctly trip circuit breaker on unsuccessful request" in {
      val mockResponse = mock[WSResponse]
      when(mockResponse.status).thenReturn(400)
      val requestSender = () => Future.successful(mockResponse)
      val routedRequest = RoutedRequest(requestSender)

      restCircuit ! routedRequest
      expectMsg(mockResponse)

      withClue("subsequent request to confirm tripped circuit breaker") {
        restCircuit ! routedRequest
        val failure = expectMsgClass(classOf[Failure])
        failure.cause.isInstanceOf[CircuitBreakerOpenException] shouldBe true
      }
    }

    "ignore a non RoutedRequest message" in {
      restCircuit ! Json.obj("key" -> "value")
      expectNoMessage()
    }
  }

}
