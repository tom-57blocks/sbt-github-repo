package xyz.jia.scala.commons.playutils.http

import scala.concurrent.duration.{DurationLong, FiniteDuration}

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, spy, verify}
import org.scalatest.Inside.inside
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import xyz.jia.scala.commons.playutils.http.ControllerUtils.Attrs

class CommonAttributesInclusionFilterSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "CommonAttributesInclusionFilter" should {
    "correctly set all expected attributes" in {
      val spyFilter = spy(new CommonAttributesInclusionFilter())

      val requestId = "fba8afc3-e303-42ad-ad89-e6ea01bff9f6"
      val processingStartTime: FiniteDuration = 28786134497083L.nanos
      doReturn(requestId).when(spyFilter).getRequestId(any[RequestHeader]())
      doReturn(processingStartTime).when(spyFilter).systemNanoTime

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val action = stubControllerComponents().actionBuilder { req =>
        val processingTime = req.attrs.get(Attrs.processingStartTime).map(_.toNanos)
        val requestId = req.attrs.get(Attrs.requestId)

        Ok(Json.obj("processingStartTime" -> processingTime, "requestId" -> requestId))
      }

      val result = spyFilter(action)(request).run()
      inside(contentAsJson(result)(defaultAwaitTimeout)) { responseJson: JsValue =>
        (responseJson \ "processingStartTime").as[Long] mustEqual processingStartTime.toNanos
        (responseJson \ "requestId").as[String] mustEqual requestId
      }

      verify(spyFilter).systemNanoTime
      verify(spyFilter).getRequestId(request)
    }
  }

}
