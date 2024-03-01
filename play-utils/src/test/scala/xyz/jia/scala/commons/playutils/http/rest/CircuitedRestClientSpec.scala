package xyz.jia.scala.commons.playutils.http.rest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.WSResponse

import xyz.jia.scala.commons.playutils.http.rest.RestCircuitManager.UnRoutedRestRequest
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig

class CircuitedRestClientSpec
    extends TestKit(ActorSystem("CircuitedRestClientSpec"))
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures
    with MockitoSugar
    with Matchers {

  implicit val patience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  implicit val maskConfig: JsonMaskConfig = JsonMaskConfig(Nil)

  implicit val circuitConfig: RestCircuitConfig =
    RestCircuitConfig(5, 10.seconds, 1.minute, Set(200))

  var mockRestClient: RestClient = _

  var circuitedRestClient: CircuitedRestClient = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    mockRestClient = mock[RestClient]
    circuitedRestClient = new CircuitedRestClient(mockRestClient)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  "get" should {
    val url = "test"
    val headers = Map("header" -> "value")
    val queryParams = Map("query" -> "value")
    val timeout = 10.seconds
    val auth = Some(AuthConfig("user", "pass", BASIC))

    "correctly make a GET request" in {
      val mockResponse = mock[WSResponse]

      val spyClient = spy(circuitedRestClient)
      doReturn(Future.successful(mockResponse))
        .when(spyClient)
        .executeRequest(anyString(), any[() => Future[WSResponse]](), any[Option[String]])(
          any[ExecutionContext](),
          any[RestCircuitConfig]()
        )

      whenReady(spyClient.get(url, headers, queryParams, timeout, auth)) { response =>
        response mustEqual mockResponse

        verify(spyClient).executeRequest(
          ArgumentMatchers.eq(url),
          ArgumentMatchers.isA(classOf[() => Future[WSResponse]]),
          ArgumentMatchers.eq(None)
        )(ArgumentMatchers.eq(ExecutionContext.global), ArgumentMatchers.eq(circuitConfig))
      }
    }
  }

  "post" should {
    val url = "test"
    val body = Json.obj("key" -> "value")
    val headers = Map("header" -> "value")
    val queryParams = Map("query" -> "value")
    val timeout = 10.seconds
    val auth = Some(AuthConfig("user", "pass", BASIC))

    "correctly make a POST request" in {
      val mockResponse = mock[WSResponse]

      val spyClient = spy(circuitedRestClient)
      doReturn(Future.successful(mockResponse))
        .when(spyClient)
        .executeRequest(anyString(), any[() => Future[WSResponse]](), any[Option[String]])(
          any[ExecutionContext](),
          any[RestCircuitConfig]()
        )

      whenReady(spyClient.post(url, body, headers, queryParams, timeout, auth)) { response =>
        response mustEqual mockResponse

        verify(spyClient).executeRequest(
          ArgumentMatchers.eq(url),
          ArgumentMatchers.isA(classOf[() => Future[WSResponse]]),
          ArgumentMatchers.eq(None)
        )(ArgumentMatchers.eq(ExecutionContext.global), ArgumentMatchers.eq(circuitConfig))
      }
    }
  }

  "executeRequest" should {
    val url = "test"
    "send request to circuit manager and return response" in {
      val mockRequestSender = mock[() => Future[WSResponse]]
      val expectedRequest =
        UnRoutedRestRequest(url, mockRequestSender, circuitConfig, ExecutionContext.global)
      val mockResponse = mock[WSResponse]
      when(mockResponse.status).thenReturn(200)

      val circuitManager = system.actorOf(
        Props(new RestCircuitManager() {
          override def receive: Receive = { case request: UnRoutedRestRequest =>
            request shouldEqual expectedRequest
            sender() ! mockResponse
          }
        }),
        RestCircuitManager.defaultName
      )

      val spyClient = spy(circuitedRestClient)
      doReturn(circuitManager)
        .when(spyClient)
        .requestManager

      whenReady(spyClient.executeRequest(url, mockRequestSender)) { response =>
        response shouldEqual mockResponse

        verify(spyClient).requestManager
      }
    }

    "throw UnsupportedApiResponseException on unsuccessful request" in {
      val mockRequestSender = mock[() => Future[WSResponse]]
      val expectedRequest =
        UnRoutedRestRequest(url, mockRequestSender, circuitConfig, ExecutionContext.global)
      val mockResponse = mock[WSResponse]
      when(mockResponse.status).thenReturn(400)

      val circuitManager = system.actorOf(Props(new RestCircuitManager() {
        override def receive: Receive = { case request: UnRoutedRestRequest =>
          request shouldEqual expectedRequest
          sender() ! mockResponse
        }
      }))

      val spyClient = spy(circuitedRestClient)
      doReturn(circuitManager)
        .when(spyClient)
        .requestManager

      whenReady(spyClient.executeRequest(url, mockRequestSender).failed) { exception =>
        exception.isInstanceOf[UnsupportedApiResponseException] shouldBe true

        val message = "Unsupported response status returned. status => '400'"
        val unsupportedApiResponseException =
          exception.asInstanceOf[UnsupportedApiResponseException]
        unsupportedApiResponseException.message shouldEqual message
        unsupportedApiResponseException.response shouldEqual mockResponse

        verify(spyClient).requestManager
      }
    }
  }

}
