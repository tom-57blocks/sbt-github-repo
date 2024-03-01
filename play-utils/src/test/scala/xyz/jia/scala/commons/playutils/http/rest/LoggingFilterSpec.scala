package xyz.jia.scala.commons.playutils.http.rest

import java.net.URI

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws._

import xyz.jia.scala.commons.playutils.json.JsonMaskConfig

class LoggingFilterSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with MockitoSugar {

  implicit override val patience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  implicit val maskConfig: JsonMaskConfig = JsonMaskConfig(List("body", "headers"))

  implicit def mat: Materializer = Materializer(system)

  var loggingFilter: LoggingFilter = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    loggingFilter = new LoggingFilter()
  }

  "extractRequestDetails" when {
    val url = "test"
    val headers = Map("header" -> Seq("value"))
    val queryParams = Map("query" -> Seq("value"))
    val requestDetails = Json.obj(
      "url" -> url,
      "headers" -> headers,
      "queryString" -> queryParams
    )

    def setupMockRequest: StandaloneWSRequest = {
      val mockRequest = mock[StandaloneWSRequest]
      when(mockRequest.url).thenReturn(url)
      when(mockRequest.headers).thenReturn(headers)
      when(mockRequest.queryString).thenReturn(queryParams)
      mockRequest
    }

    "given a request with an empty body" should {
      "correctly extract request details" in {
        val mockRequest = setupMockRequest
        when(mockRequest.body).thenReturn(EmptyBody)
        whenReady(loggingFilter.extractRequestDetails(mockRequest)) { result =>
          result shouldEqual requestDetails
        }
      }
    }

    "given a request with a JSON body" should {
      "correctly extract request details" in {
        val jsonBody = Json.obj("key" -> "value")
        val requestDetailsWithJsonBody = requestDetails + ("body" -> jsonBody)
        val requestBodyWithJson = InMemoryBody(ByteString.fromString(jsonBody.toString()))
        val mockRequest = setupMockRequest
        when(mockRequest.body).thenReturn(requestBodyWithJson)

        whenReady(loggingFilter.extractRequestDetails(mockRequest)) { result =>
          result shouldEqual requestDetailsWithJsonBody
        }
      }
    }

    "given a request with a non-JSON body" should {
      "correctly extract request details" in {
        val textBody = "text-body"
        val unableToParseJson = "Body cannot be parsed as JSON for masking"
        val requestDetailsWithTextBody = requestDetails + ("body" -> JsString(unableToParseJson))
        val requestBodyWithText = SourceBody(Source.single(ByteString.fromString(textBody)))
        val mockRequest = setupMockRequest
        when(mockRequest.body).thenReturn(requestBodyWithText)

        whenReady(loggingFilter.extractRequestDetails(mockRequest)) { result =>
          result shouldEqual requestDetailsWithTextBody
        }
      }
    }

    "given a request with a non sensitive non-JSON body" should {
      "correctly extract request details" in {
        val textBody = "text-body"
        val requestDetailsWithTextBody = requestDetails + ("body" -> JsString(textBody))
        val requestBodyWithText = SourceBody(Source.single(ByteString.fromString(textBody)))
        val mockRequest = setupMockRequest
        when(mockRequest.body).thenReturn(requestBodyWithText)

        val spyFilter = spy(loggingFilter)
        doReturn(false).when(spyFilter).hasSensitivePayload

        whenReady(spyFilter.extractRequestDetails(mockRequest)) { result =>
          result shouldEqual requestDetailsWithTextBody
        }
      }
    }
  }

  "extractResponseDetails" when {
    val url = "test"
    val headers = Map("header" -> Seq("value"))
    val responseDetails = Json.obj(
      "url" -> url,
      "headers" -> headers
    )

    def setupMockResponse: StandaloneWSResponse = {
      val mockResponse = mock[StandaloneWSResponse]
      when(mockResponse.uri).thenReturn(URI.create(url))
      when(mockResponse.headers).thenReturn(headers)
      mockResponse
    }

    "given a response with an empty body" should {
      "correctly extract response details" in {
        val responseDetailsWithEmptyBody = responseDetails + ("body" -> JsString(""))
        val mockResponse = setupMockResponse
        doReturn(Source.empty[ByteString]).when(mockResponse).bodyAsSource

        whenReady(loggingFilter.extractResponseDetails(mockResponse)) { case (response, result) =>
          response shouldEqual mockResponse
          result shouldEqual responseDetailsWithEmptyBody
        }
      }
    }

    "given a response with a JSON body" should {
      "correctly extract response details" in {
        val jsonBody = Json.obj("key" -> "value")
        val responseDetailsWithJsonBody = responseDetails + ("body" -> jsonBody)
        val responseBodySource = Source.single(ByteString.fromString(jsonBody.toString()))
        val mockResponse = setupMockResponse
        doReturn(responseBodySource).when(mockResponse).bodyAsSource

        whenReady(loggingFilter.extractResponseDetails(mockResponse)) { case (response, result) =>
          response shouldEqual mockResponse
          result shouldEqual responseDetailsWithJsonBody
        }
      }
    }

    "given a response with a non-JSON body" should {
      "correctly extract response details" in {
        val textBody = "text-body"
        val unableToParseJson = "Body cannot be parsed as JSON for masking"
        val responseDetailsWithTextBody = responseDetails + ("body" -> JsString(unableToParseJson))
        val responseBodySource = Source.single(ByteString.fromString(textBody))
        val mockResponse = setupMockResponse
        doReturn(responseBodySource).when(mockResponse).bodyAsSource

        whenReady(loggingFilter.extractResponseDetails(mockResponse)) { case (response, result) =>
          response shouldEqual mockResponse
          result shouldEqual responseDetailsWithTextBody
        }
      }
    }

    "given a response with a non sensitive non-JSON body" should {
      "correctly extract response details" in {
        val textBody = "text-body"
        val responseDetailsWithTextBody = responseDetails + ("body" -> JsString(textBody))
        val responseBodySource = Source.single(ByteString.fromString(textBody))
        val mockResponse = setupMockResponse
        doReturn(responseBodySource).when(mockResponse).bodyAsSource

        val spyFilter = spy(loggingFilter)
        doReturn(false).when(spyFilter).hasSensitivePayload

        whenReady(spyFilter.extractResponseDetails(mockResponse)) { case (response, result) =>
          response shouldEqual mockResponse
          result shouldEqual responseDetailsWithTextBody
        }
      }
    }
  }

  "parseBody" when {
    "given a JSON body" should {
      "correctly parse the body" in {
        val jsonBody = Json.obj("key" -> "value")
        val jsonBodyByteString = ByteString.fromString(jsonBody.toString())
        loggingFilter.parseBody(Json.obj(), jsonBodyByteString) shouldEqual Json.obj(
          "body" -> jsonBody
        )
      }
    }

    "given a non-JSON body" should {
      "correctly parse the body" in {
        val textBody = "text-body"
        val textBodyByteString = ByteString.fromString(textBody)
        loggingFilter.parseBody(Json.obj(), textBodyByteString) shouldEqual Json.obj(
          "body" -> JsString("Body cannot be parsed as JSON for masking")
        )
      }
    }

    "given a non sensitive non-JSON body" should {
      "correctly parse the body" in {
        val textBody = "text-body"
        val textBodyByteString = ByteString.fromString(textBody)

        val spyFilter = spy(loggingFilter)
        doReturn(false).when(spyFilter).hasSensitivePayload

        spyFilter.parseBody(Json.obj(), textBodyByteString) shouldEqual Json.obj(
          "body" -> JsString(textBody)
        )
      }
    }
  }

}
