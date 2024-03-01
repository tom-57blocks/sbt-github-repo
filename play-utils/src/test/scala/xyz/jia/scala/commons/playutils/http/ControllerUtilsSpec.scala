package xyz.jia.scala.commons.playutils.http

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.io.Source
import scala.util.Try

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, never, spy, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, empty, not}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json._
import play.api.mvc.{Headers, RequestHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import xyz.jia.scala.commons.playutils.http.ControllerUtils.Attrs

class ControllerUtilsSpec extends AnyWordSpec with ScalaFutures with GuiceOneAppPerTest {

  implicit def mat: Materializer = app.materializer

  class RequestUtilsStub extends ControllerUtils

  "generateRequestId" should {
    "correctly generate a request ID" in {
      Try(UUID.fromString(ControllerUtils.generateRequestId)).toOption must not be empty
    }
  }

  "getRequestId" should {
    val attributeValue = "e03dfd4a-5500-4660-b5d5-2dc5131f920e"
    val headerValue = "9bcf3f01-bcfd-4693-8760-945f51f6286d"

    "get the request ID set in the request attributes" in {
      val request =
        FakeRequest()
          .addAttr(Attrs.requestId, attributeValue)
          .withHeaders(Headers.apply(ControllerUtils.requestIdHeaderName -> headerValue))

      ControllerUtils.getRequestId(request) mustEqual attributeValue
    }

    "get the request ID set in the request headers" in {
      val request =
        FakeRequest().withHeaders(Headers.apply(ControllerUtils.requestIdHeaderName -> headerValue))

      ControllerUtils.getRequestId(request) mustEqual headerValue
    }

    "generate a new request ID" in {
      val request = FakeRequest()
      val spyRequestUtils = spy(new RequestUtilsStub)
      val generateRequestId = "e0652e2e-10bb-440b-af27-41fd02d9bdba"
      doReturn(generateRequestId).when(spyRequestUtils).generateRequestId
      spyRequestUtils.getRequestId(request) mustEqual generateRequestId
    }
  }

  "getExecutionTime" should {
    val startTimeArg = System.nanoTime().nanos
    val startTimeAttribute = startTimeArg.minus(18.minutes)
    val endTime = startTimeArg.plus(2.minutes)
    val request = FakeRequest().addAttr(Attrs.processingStartTime, startTimeAttribute)

    "compute the processing time using the provided start time" in {
      val spyRequestUtils = spy(new RequestUtilsStub)
      doReturn(endTime).when(spyRequestUtils).systemNanoTime
      spyRequestUtils.getExecutionTime(request, Some(startTimeArg)) mustBe Some(2.minutes.toMillis)
    }

    "compute the processing time using the processingStart time attribute" in {
      val spyRequestUtils = spy(new RequestUtilsStub)
      doReturn(endTime).when(spyRequestUtils).systemNanoTime
      spyRequestUtils.getExecutionTime(request, None) mustBe Some(20.minutes.toMillis)
    }

    "return no processing time when the start time cannot be determined" in {
      val spyRequestUtils = spy(new RequestUtilsStub)
      doReturn(endTime).when(spyRequestUtils).systemNanoTime
      spyRequestUtils.getExecutionTime(FakeRequest(), None) mustBe None
    }
  }

  "generateResponse" should {
    val startTimeArg = System.nanoTime().nanos
    val endTime = startTimeArg.plus(1834.millis)
    val requestId = "411d6fe6-5065-463a-9708-531e80dc4379"
    val payloadResource = Source.fromResource("http/sampleServiceResponsePayload.json")
    val responsePayloadJson = Json.parse(payloadResource.getLines().mkString("\n"))

    def setupSpy: RequestUtilsStub = {
      val spyRequestUtils = spy(new RequestUtilsStub)

      doReturn(endTime).when(spyRequestUtils).systemNanoTime
      doReturn(requestId).when(spyRequestUtils).getRequestId(any[RequestHeader]())

      spyRequestUtils
    }
    val request = FakeRequest()

    "generate the right payload when all arguments are provided" in {
      val spyRequestUtils = setupSpy
      val response: Result = spyRequestUtils.generateResponse(
        request = request,
        resultCode = "2000",
        httpStatusCode = 200,
        data = Json.obj("id" -> 160),
        errors = Some(JsArray(Seq(JsString("name.required"), JsString("age.invalid")))),
        startTime = Some(startTimeArg),
        requestId = Some(requestId)
      )

      contentAsJson(Future.successful(response)) mustEqual responsePayloadJson
      verify(spyRequestUtils).getExecutionTime(request, Some(startTimeArg))
      verify(spyRequestUtils, never()).getRequestId(any[RequestHeader]())
    }

    "generate the right payload when the request ID is not provided" in {
      val spyRequestUtils = setupSpy
      val response: Result = spyRequestUtils.generateResponse(
        request = request,
        resultCode = "2000",
        httpStatusCode = 202,
        data = Json.obj("id" -> 160),
        errors = Some(JsArray(Seq(JsString("name.required"), JsString("age.invalid")))),
        startTime = Some(startTimeArg)
      )

      status(Future.successful(response)) mustBe ACCEPTED
      contentAsJson(Future.successful(response)) mustEqual responsePayloadJson
      verify(spyRequestUtils).getRequestId(any[RequestHeader]())
    }

    "gracefully handle missing execution processing start time" in {
      val response = setupSpy.generateResponse(FakeRequest(), "2000", 200)
      status(Future.successful(response)) mustBe OK
      (contentAsJson(Future.successful(response)) \ "meta" \ "executionTime").as[Long] mustEqual -1
    }

    "generate the right payload when the errors are not provided" in {
      val response: Result = setupSpy.generateResponse(
        request = request,
        resultCode = "2000",
        httpStatusCode = 202,
        data = Json.obj("id" -> 160),
        startTime = Some(startTimeArg)
      )

      status(Future.successful(response)) mustBe ACCEPTED
      val responseWithoutErrors = (__ \ "meta" \ "errors").prune(responsePayloadJson).get
      contentAsJson(Future.successful(response)) mustEqual responseWithoutErrors
    }

    "generate the right payload when the data is not provided" in {
      val response: Result = setupSpy.generateResponse(
        request = request,
        resultCode = "2000",
        httpStatusCode = 202,
        errors = Some(JsArray(Seq(JsString("name.required"), JsString("age.invalid")))),
        startTime = Some(startTimeArg)
      )

      status(Future.successful(response)) mustBe ACCEPTED
      val responseWithNullData =
        responsePayloadJson.as[JsObject] ++ Json.obj("data" -> JsNull)
      contentAsJson(Future.successful(response)) mustEqual responseWithNullData
    }
  }

  case class Dummy(telephone: String)

  implicit val jsonReads: OFormat[Dummy] = Json.format[Dummy]

  "withJsonPayloadValidation" should {
    val validRequest = FakeRequest(
      method = "GET",
      uri = "/user/search",
      headers = Headers(("X-Request-Id", "e5e57c3e-9d52-46ae-b67a-6dccb5a0b15d")),
      body = Json.obj("telephone" -> "1235")
    )

    "correctly process a valid request" in {
      val mockResult = Future.successful(mock[Result])
      ControllerUtils.withJsonPayloadValidation(validRequest) { _: Dummy =>
        mockResult
      } mustEqual mockResult
    }

    "correctly process an invalid request" in {
      //      class DummyControllerUtils extends ControllerUtils
      val invalidRequest = validRequest.withBody(JsObject.empty)
      val result =
        ControllerUtils.withJsonPayloadValidation(invalidRequest) { _: Dummy =>
          Future.successful(mock[Result])
        }

      whenReady(result.flatMap(_.body.consumeData)) { content =>
        val invalidResponsePath = "http/invalidDummyRequestResponsePayload.json"
        val invalidResponseJson =
          Json.parse(Source.fromResource(invalidResponsePath).getLines().mkString("\n"))

        Json.parse(content.utf8String) mustEqual invalidResponseJson
      }
    }
  }

}
