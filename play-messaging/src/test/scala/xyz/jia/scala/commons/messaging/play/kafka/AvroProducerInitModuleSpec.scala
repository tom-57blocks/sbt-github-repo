package xyz.jia.scala.commons.messaging.play.kafka

import scala.concurrent.ExecutionContext

import com.google.inject.Provider
import com.google.inject.binder.AnnotatedBindingBuilder
import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{doReturn, never, spy, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.{Application, Configuration, Environment}
import xyz.jia.scala.commons.messaging.kafka.AvroProducer
import xyz.jia.scala.commons.play.DummyTestExecutionContext

class AvroProducerInitModuleSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  var mockEnvironment: Environment = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockEnvironment = mock[Environment]
  }

  val producersConfig: Config = ConfigFactory.parseResources("kafka/avroProducers.conf").resolve()

  val maskConfig: Config = producersConfig.getConfig("loggingMaskConfigContainer")

  "instantiateProducer" should {
    val producerConfiguration = Configuration(
      producersConfig.getConfig("kafka.injectable.producer.avro.validTestProducer1")
    )

    "correctly initialize an Avro Producer" in {
      val spyInitModule =
        spy(new AvroProducerInitModule(mockEnvironment, new Configuration(maskConfig)))
      val mockExecutionContext = mock[ExecutionContext]
      val mockProducerBinding = mock[AnnotatedBindingBuilder[AvroProducer]]
      val mockApplicationProvider = mock[Provider[Application]]
      doReturn(mockExecutionContext)
        .when(spyInitModule)
        .findClassInstance[ExecutionContext](any[String](), any[Provider[Application]]())
      doReturn(mockProducerBinding).when(spyInitModule).producerBinding

      val producer =
        spyInitModule.instantiateProducer(producerConfiguration, mockApplicationProvider)
      producer.producerConfig mustEqual producerConfiguration.underlying
      producer.executionContext mustEqual mockExecutionContext
      verify(spyInitModule).findClassInstance[ExecutionContext](
        eqTo(classOf[DummyTestExecutionContext].getName),
        eqTo(mockApplicationProvider)
      )

      // Never directly call the application provider method
      verify(spyInitModule, never()).applicationProvider
    }
  }

  "producerBinding" should {
    "return the right Avro Producer binding" in {
      class AvroProducerInitModuleBinderExposer(
          environment: Environment,
          configuration: Configuration
      ) extends AvroProducerInitModule(environment, configuration) {
        override def bind[T](clazz: Class[T]): AnnotatedBindingBuilder[T] = super.bind(clazz)
      }

      val spyInitModule =
        spy(new AvroProducerInitModuleBinderExposer(mockEnvironment, new Configuration(maskConfig)))
      doReturn(mock[AnnotatedBindingBuilder[AvroProducer]])
        .when(spyInitModule)
        .bind(classOf[AvroProducer])

      spyInitModule.producerBinding
      verify(spyInitModule).bind(classOf[AvroProducer])
    }
  }

  "applicationProvider" should {
    "return the correct application provider" in {
      class AvroProducerInitModuleBinderExposer(
          environment: Environment,
          configuration: Configuration
      ) extends AvroProducerInitModule(environment, configuration) {
        override def getProvider[T](`type`: Class[T]): Provider[T] = super.getProvider(`type`)
      }

      val mockApplicationProvider = mock[Provider[Application]]
      val spyInitModule =
        spy(new AvroProducerInitModuleBinderExposer(mockEnvironment, new Configuration(maskConfig)))
      doReturn(mockApplicationProvider)
        .when(spyInitModule)
        .getProvider[Application](any[Class[Application]]())

      spyInitModule.applicationProvider mustEqual mockApplicationProvider
      verify(spyInitModule).getProvider(classOf[Application])
    }
  }

}
