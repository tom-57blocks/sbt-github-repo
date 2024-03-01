package xyz.jia.scala.commons.playutils

import akka.Done
import com.google.inject.AbstractModule
import org.mockito.Mockito.{doReturn, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Inside.inside
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.Injector
import play.api.mvc.Filter

import xyz.jia.scala.commons.playutils.http.CommonAttributesInclusionFilter

class RuntimeDependencyLookupServiceSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with MockitoSugar
    with ScalaFutures {

  var dependencyLookupService: RuntimeDependencyLookupService = _

  var mockApplication: Application = _

  var mockInjector: Injector = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockApplication = mock[Application]
    mockInjector = mock[Injector]
    when(mockApplication.injector).thenReturn(mockInjector)

    dependencyLookupService = new RuntimeDependencyLookupService(() => mockApplication)
  }

  "lookup" should {
    "find and return an instance of an object from the application's injector" in {
      val mockService = mock[RuntimeDependencyLookupService]
      val className = classOf[RuntimeDependencyLookupService].getName
      doReturn(mockService).when(mockInjector).instanceOf(Class.forName(className))

      dependencyLookupService.lookup[RuntimeDependencyLookupService](
        className,
        this.getClass.getClassLoader
      ) mustEqual mockService
      verify(mockInjector).instanceOf(Class.forName(className))
    }
  }

  "validateCompatibility" should {
    "return right for a class that exists and is a descendant of specified class" in {
      RuntimeDependencyLookupService.validateCompatibility(
        classOf[CommonAttributesInclusionFilter].getName,
        this.getClass.getClassLoader,
        classOf[Filter]
      ) mustBe Right(Done)
    }

    "return left for a class that exists but is not a descendant of specified class" in {
      val className = classOf[CommonAttributesInclusionFilter].getName
      val validationResult = RuntimeDependencyLookupService.validateCompatibility(
        className,
        this.getClass.getClassLoader,
        classOf[AbstractModule]
      )
      inside(validationResult.swap.toOption) { case exception: Option[Throwable] =>
        exception.isDefined mustBe true
        val abstractModuleClassName = classOf[AbstractModule].getName
        exception.get.getMessage mustEqual
          s"$className is not of the same type or a descendant of $abstractModuleClassName"
      }
    }

    "return left for a class that does not exist" in {
      val className = "chicken_sandwich"
      val validationResult = RuntimeDependencyLookupService.validateCompatibility(
        className,
        this.getClass.getClassLoader,
        classOf[AbstractModule]
      )
      inside(validationResult.swap.toOption) { case exception: Option[Throwable] =>
        exception.isDefined mustBe true
        exception.get.getClass mustEqual classOf[ClassNotFoundException]
      }
    }
  }

}
