package xyz.jia.scala.commons.playutils.tasks

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, spy, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import xyz.jia.scala.commons.playutils.FileUtils
import xyz.jia.scala.commons.playutils.http.ControllerUtils
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.playutils.tasks.TaskErrorCode.ValidationFailed
import xyz.jia.scala.commons.utils.ErrorMessage

class HttpInvocableTaskControllerSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with ScalaFutures
    with BeforeAndAfterEach {

  implicit def mat: Materializer = Materializer(system)

  implicit val jonMaskConfig: JsonMaskConfig = JsonMaskConfig(Nil)

  var mockHttpInvocableTaskService: HttpInvocableTaskService = _

  var controller: HttpInvocableTaskController = _

  class DummyHttpInvocableTaskController(
      cc: ControllerComponents,
      taskService: HttpInvocableTaskService
  )(implicit ec: ExecutionContext, maskConfig: JsonMaskConfig)
      extends HttpInvocableTaskController(taskService, cc, maskConfig, ec)

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockHttpInvocableTaskService = mock[HttpInvocableTaskService]

    controller = new DummyHttpInvocableTaskController(
      stubControllerComponents(),
      mockHttpInvocableTaskService
    )
  }

  "executeTask" should {
    def setupController: HttpInvocableTaskController = {
      val spyController = spy(controller)
      doReturn(Some(89L))
        .when(spyController)
        .getExecutionTime(any[RequestHeader](), any[Option[FiniteDuration]]())
      spyController
    }

    val request =
      FakeRequest(POST, "/task")
        .withJsonBody(FileUtils.getResourceAsJson("tasks/sampleValidTaskRequest.json").get)
        .withHeaders(ControllerUtils.requestIdHeaderName -> "b5866235-df5e-4cc7-a33a-4efd")

    "correctly process a valid request" in {
      when(mockHttpInvocableTaskService.processTask(any[HttpInvocableTaskRequest]()))
        .thenReturn(Future.successful(Right(Done)))

      val spyController = setupController
      whenReady(call(spyController.executeTask, request)) { result =>
        result.header.status mustEqual ACCEPTED
        whenReady(result.body.consumeData) { content =>
          val responseFilePath = "tasks/sampleAcceptedTaskResponse.json"
          Json.parse(content.utf8String) mustEqual FileUtils.getResourceAsJson(responseFilePath).get
        }
      }
    }

    "correctly handle a rejected request" in {
      val messages = Seq("xyz lmn", "lorem ipsum").map(ErrorMessage)
      when(mockHttpInvocableTaskService.processTask(any[HttpInvocableTaskRequest]()))
        .thenReturn(Future.successful(Left((messages, ValidationFailed))))

      val spyController = setupController
      whenReady(call(spyController.executeTask, request)) { result =>
        result.header.status mustEqual BAD_REQUEST
        whenReady(result.body.consumeData) { content =>
          val responseFilePath = "tasks/badRequestTaskResponse.json"
          Json.parse(content.utf8String) mustEqual FileUtils.getResourceAsJson(responseFilePath).get
        }
      }
    }
  }

}
