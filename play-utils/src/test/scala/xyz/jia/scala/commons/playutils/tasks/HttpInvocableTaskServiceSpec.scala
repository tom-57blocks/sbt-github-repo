package xyz.jia.scala.commons.playutils.tasks

import java.time.OffsetDateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import akka.Done
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.{JsNull, JsValue}

import xyz.jia.scala.commons.utils.ErrorMessage

class HttpInvocableTaskServiceSpec
    extends AnyWordSpec
    with ScalaFutures
    with BeforeAndAfterEach
    with Matchers {

  var httpInvocableTaskService: HttpInvocableTaskService = _

  var mockTaskLoader: HttpInvocableTaskLoader = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockTaskLoader = mock[HttpInvocableTaskLoader]
    httpInvocableTaskService = new HttpInvocableTaskService(mockTaskLoader)
  }

  "processTask" when {
    val request = HttpInvocableTaskRequest("loremIpsum", JsNull, "abc-efg")
    "a specified task exists and metadata validation passes" should {
      val testMetadata: OffsetDateTime = OffsetDateTime.parse("2022-05-05T03:51:50+03:00")

      def setupCommonMocks(dummyTask: HttpInvocableTask): HttpInvocableTaskRequestRepo = {
        val mockRepo = mock[HttpInvocableTaskRequestRepo]
        when(mockTaskLoader.getTask(anyString())).thenReturn(Some(dummyTask))
        when(mockTaskLoader.requestsRepo).thenReturn(mockRepo)

        when(mockRepo.handleSuccessfulProcessing(any[HttpInvocableTaskRequest]()))
          .thenReturn(Future.successful(Done))
        when(mockRepo.handleFailedProcessing(any[HttpInvocableTaskRequest](), any[Throwable]()))
          .thenReturn(Future.successful(Done))
        mockRepo
      }

      "correctly handle successful task saving and processing" in {
        var processedData: Option[OffsetDateTime] = None
        val task: HttpInvocableTask = new HttpInvocableTask {
          override type DataType = OffsetDateTime

          override def parseData(data: JsValue): Future[Either[Seq[ErrorMessage], DataType]] =
            Future.successful(Right(testMetadata))

          override def process(data: DataType): Future[Done] = {
            processedData = Some(data)
            Future.successful(Done)
          }
        }

        val mockRepo = setupCommonMocks(task)

        when(mockRepo.saveRequest(any[HttpInvocableTaskRequest]()))
          .thenReturn(Future.successful(Right(Done)))

        whenReady(httpInvocableTaskService.processTask(request)) { result =>
          result mustBe Right(Done)
          verify(mockTaskLoader).getTask(request.task)
          verify(mockRepo).saveRequest(request)
          verify(mockRepo).handleSuccessfulProcessing(request)
          // Hack because Mockito cannot detect/intercept HttpInvocableTask#process invocations
          processedData mustBe Some(testMetadata)
        }
      }
      "correctly handle successful task saving and failed processing" in {
        val exception = new RuntimeException("Faked Oops!")
        val task: HttpInvocableTask = new HttpInvocableTask {
          override type DataType = OffsetDateTime

          override def parseData(data: JsValue): Future[Either[Seq[ErrorMessage], DataType]] =
            Future.successful(Right(testMetadata))

          override def process(data: DataType): Future[Done] = Future.failed(exception)
        }

        val mockRepo = setupCommonMocks(task)

        when(mockRepo.saveRequest(any[HttpInvocableTaskRequest]()))
          .thenReturn(Future.successful(Right(Done)))

        whenReady(httpInvocableTaskService.processTask(request)) { result =>
          result mustBe Right(Done)
          verify(mockTaskLoader).getTask(request.task)
          verify(mockRepo).saveRequest(request)
          verify(mockRepo, never()).handleSuccessfulProcessing(any[HttpInvocableTaskRequest]())
          verify(mockRepo).handleFailedProcessing(request, exception)
        }
      }

      "correctly handle failure to save a task" in {
        val mockDummyTask = mock[DummyTask]
        when(mockDummyTask.parseData(any[JsValue]()))
          .thenReturn(Future.successful(Right(testMetadata)))

        val mockRepo = setupCommonMocks(mockDummyTask)

        val errorMessages = Seq("b", "x").map(ErrorMessage)
        when(mockRepo.saveRequest(any[HttpInvocableTaskRequest]()))
          .thenReturn(Future.successful(Left((errorMessages, TaskErrorCode.InternalError))))
        when(mockDummyTask.process(any[OffsetDateTime]())).thenReturn(Future.successful(Done))

        whenReady(httpInvocableTaskService.processTask(request)) { result =>
          result mustBe Left((errorMessages, TaskErrorCode.InternalError))
          verify(mockDummyTask).parseData(request.meta)
          verify(mockRepo).saveRequest(request)
          verify(mockDummyTask, never()).process(any[OffsetDateTime]())
        }
      }
    }

    "a specified task exists but metadata validation fails" should {
      "return the metadata validation errors" in {
        val mockTask = mock[DummyTask]
        val errorMessages = Seq("jk", "tyi").map(ErrorMessage)

        when(mockTaskLoader.getTask(anyString())).thenReturn(Some(mockTask))
        when(mockTask.parseData(any[JsValue]())).thenReturn(Future.successful(Left(errorMessages)))

        whenReady(httpInvocableTaskService.processTask(request)) { result =>
          result mustBe Left((errorMessages, TaskErrorCode.ValidationFailed))
          verify(mockTaskLoader).getTask(request.task)
          verify(mockTask).parseData(request.meta)
          verifyNoMoreInteractions(mockTask)
          verifyNoMoreInteractions(mockTaskLoader)
        }
      }
    }

    "a specified task doe not exist" should {
      "return an error" in {
        when(mockTaskLoader.getTask(anyString())).thenReturn(None)
        whenReady(httpInvocableTaskService.processTask(request)) { result =>
          val expectedError = ErrorMessage(s"Could not find requested task ${request.task}")
          result mustBe Left((Seq(expectedError), TaskErrorCode.ValidationFailed))
          verify(mockTaskLoader).getTask(request.task)
          verifyNoMoreInteractions(mockTaskLoader)
        }
      }
    }
  }

}
