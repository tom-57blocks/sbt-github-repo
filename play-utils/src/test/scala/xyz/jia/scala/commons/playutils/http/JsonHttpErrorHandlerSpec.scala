package xyz.jia.scala.commons.playutils.http

import scala.concurrent.duration.FiniteDuration
import scala.io.Source

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, spy}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest

class JsonHttpErrorHandlerSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerTest
    with ScalaFutures {

  implicit def mat: Materializer = app.materializer

  "JsonHttpErrorHandler" should {

    def setupHandler: JsonHttpErrorHandler = {
      val errorHandler = spy(new JsonHttpErrorHandler)
      doReturn(Some(20L))
        .when(errorHandler)
        .getExecutionTime(any[RequestHeader](), any[Option[FiniteDuration]]())
      doReturn("9082fc19-824f-4438-9a52-9362639927b5")
        .when(errorHandler)
        .getRequestId(any[RequestHeader]())
      doReturn("93626399")
        .when(errorHandler)
        .generateErrorId
      errorHandler
    }

    "correctly generate response on client error" in {
      val request = FakeRequest()
      val statusCode = BAD_REQUEST
      val message = "Client Error"

      val handler = setupHandler
      whenReady(handler.onClientError(request, statusCode, message)) { result: Result =>
        result.header.status mustBe statusCode
        whenReady(result.body.consumeData) { content =>
          val clientErrorJsonPath = "http/jsonHttpErrorHandler/sampleClientError.json"
          val clientErrorJson: JsValue =
            Json.parse(Source.fromResource(clientErrorJsonPath).getLines().mkString("\n"))
          val jsonResponse: JsValue = Json.parse(content.utf8String)

          jsonResponse mustEqual clientErrorJson
        }
      }
    }

    "correctly generate response on server error" in {
      val request = FakeRequest()
      val statusCode = INTERNAL_SERVER_ERROR
      val exception = new Throwable("Server Error")

      val handler = setupHandler
      whenReady(handler.onServerError(request, exception)) { result: Result =>
        result.header.status mustBe statusCode
        whenReady(result.body.consumeData) { content =>
          val serverErrorJsonPath = "http/jsonHttpErrorHandler/sampleServerError.json"
          val serverErrorJson: JsValue =
            Json.parse(Source.fromResource(serverErrorJsonPath).getLines().mkString("\n"))
          val jsonResponse: JsValue = Json.parse(content.utf8String)

          jsonResponse mustEqual serverErrorJson
        }
      }
    }
  }

  "generateErrorId" should {

    val handler = new JsonHttpErrorHandler

    "correctly generate an error ID" in {
      handler.generateErrorId should have size 8
    }
  }

}
