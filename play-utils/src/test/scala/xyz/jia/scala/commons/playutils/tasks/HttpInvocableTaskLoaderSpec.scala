package xyz.jia.scala.commons.playutils.tasks

import com.google.inject.Provider
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.{Application, Configuration}

import xyz.jia.scala.commons.playutils.RuntimeDependencyLookupService

class HttpInvocableTaskLoaderSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  val configuration: Configuration = new Configuration(
    ConfigFactory.parseResources("tasks/sampleValidTask.conf").resolve()
  )

  val classLoader: ClassLoader = Thread.currentThread().getContextClassLoader

  var mockApplication: Application = _

  var applicationProvider: Provider[Application] = _

  var mockDependencyLookupService: RuntimeDependencyLookupService = _

  var httpInvocableTaskLoader: HttpInvocableTaskLoader = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockApplication = mock[Application]
    mockDependencyLookupService = mock[RuntimeDependencyLookupService]
    applicationProvider = () => mockApplication

    when(mockApplication.classloader).thenReturn(classLoader)

    httpInvocableTaskLoader = new HttpInvocableTaskLoader(
      configuration,
      applicationProvider,
      mockDependencyLookupService
    )
  }

  "requestsRepo" should {
    "return the valid configured repo implementation " in {
      val mockTaskRequestRepo = mock[HttpInvocableTaskRequestRepo]
      when(
        mockDependencyLookupService
          .lookup[HttpInvocableTaskRequestRepo](any[String](), any[ClassLoader]())
      ).thenReturn(mockTaskRequestRepo)

      httpInvocableTaskLoader.requestsRepo mustEqual mockTaskRequestRepo
      verify(mockDependencyLookupService).lookup[HttpInvocableTaskRequestRepo](
        "xyz.jia.scala.commons.playutils.tasks.DummyTaskStorageService",
        classLoader
      )
    }

    "throw an exception for an invalid configured repo implementation " in {
      val taskLoader = new HttpInvocableTaskLoader(
        Configuration.empty,
        applicationProvider,
        mockDependencyLookupService
      )
      val exception = intercept[IllegalArgumentException](taskLoader.requestsRepo)
      exception.getMessage.contains(
        "Invalid HTTP invocable task configuration provided"
      ) mustBe true
      verifyNoInteractions(mockDependencyLookupService)
    }
  }

  "getTask" should {
    "find and return an existing task" in {
      val mockHttpInvocableTask = mock[HttpInvocableTask]
      when(
        mockDependencyLookupService.lookup[HttpInvocableTask](anyString(), any[ClassLoader]())
      ).thenReturn(mockHttpInvocableTask)

      httpInvocableTaskLoader.getTask("demoTask") mustBe Some(mockHttpInvocableTask)
      verify(mockDependencyLookupService).lookup[HttpInvocableTaskRequestRepo](
        "xyz.jia.scala.commons.playutils.tasks.DummyTask",
        classLoader
      )
    }

    "gracefully handle a non existing task" in {
      httpInvocableTaskLoader.getTask("Task") mustBe None
      verifyNoInteractions(mockDependencyLookupService)
    }
  }

}
