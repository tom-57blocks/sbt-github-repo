package xyz.jia.scala.commons.messaging.play.kafka

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.MapHasAsJava

import com.google.inject.Provider
import com.google.inject.binder.AnnotatedBindingBuilder
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.apache.avro.specific.SpecificRecord
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.Injector
import play.api.{Application, Configuration}
import xyz.jia.scala.commons.playutils.RuntimeDependencyLookupService
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.messaging.kafka.AvroProducer

import xyz.jia.scala.commons.messaging.play.NamedKafkaProducerImpl

class ProducerInitModuleSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with MockitoSugar
    with ScalaFutures {

  class AvroProducerInitModuleStub(
      override val baseConfigKey: String,
      override val configuration: Configuration
  ) extends ProducerInitModule[String, SpecificRecord, AvroProducer] {

    override val logMaskConfig: JsonMaskConfig = JsonMaskConfig(Nil)

    override def producerBinding: AnnotatedBindingBuilder[AvroProducer] = ???

    override def instantiateProducer(
        config: Configuration,
        applicationProvider: Provider[Application]
    ): AvroProducer = ???

    override def applicationProvider: Provider[Application] = ???

  }

  var mockProducerBinding: AnnotatedBindingBuilder[AvroProducer] = _

  var mockProducer: AvroProducer = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    mockProducerBinding = mock[AnnotatedBindingBuilder[AvroProducer]]
    mockProducer = mock[AvroProducer]
  }

  "configure" when {
    val baseConfigKey = "kafka.injectable.producer.avro"

    "given valid configuration" should {
      "gracefully handle missing injectable producer config" in {
        val spyInitModule = spy(new AvroProducerInitModuleStub(baseConfigKey, Configuration.empty))

        spyInitModule.configure()
        verify(spyInitModule, never()).bindProducer(
          any[String](),
          any[Configuration](),
          any[Provider[Application]](),
          any[AnnotatedBindingBuilder[AvroProducer]]()
        )
      }

      "correctly create producer instances" in {
        val producerConfig = ConfigFactory.parseResources("kafka/avroProducers.conf").resolve()
        val configuration = new Configuration(producerConfig)
        val mockApplicationProvider = mock[Provider[Application]]

        val spyInitModule = spy(new AvroProducerInitModuleStub(baseConfigKey, configuration))

        doReturn(mockApplicationProvider).when(spyInitModule).applicationProvider
        doReturn(mockProducerBinding).when(spyInitModule).producerBinding
        doNothing()
          .when(spyInitModule)
          .bindProducer(
            any[String](),
            any[Configuration](),
            any[Provider[Application]](),
            any[AnnotatedBindingBuilder[AvroProducer]]()
          )

        spyInitModule.configure()
        verify(spyInitModule).bindProducer(
          eqTo("validTestProducer1"),
          eqTo(new Configuration(producerConfig.getConfig(s"$baseConfigKey.validTestProducer1"))),
          eqTo(mockApplicationProvider),
          eqTo(mockProducerBinding)
        )
        verify(spyInitModule).bindProducer(
          eqTo("validTestProducer2"),
          eqTo(new Configuration(producerConfig.getConfig(s"$baseConfigKey.validTestProducer2"))),
          eqTo(mockApplicationProvider),
          eqTo(mockProducerBinding)
        )
      }
    }

    "given invalid configuration" should {
      val baseConfig = "avro.producer.invalid"

      def testMisConfiguration(options: Map[String, String]): Exception = {
        val producerConfig =
          ConfigFactory
            .parseResources("kafka/avroProducers.conf")
            .withFallback(ConfigFactory.parseMap(options.asJava))
            .resolve()
        val configuration = new Configuration(producerConfig)

        intercept[Exception](new AvroProducerInitModuleStub(baseConfig, configuration).configure())
      }

      "reject creating a producer with no execution context (ec) specified" in {
        val thrownException = testMisConfiguration(Map.empty)
        thrownException.getClass mustEqual classOf[ConfigException.Missing]
        thrownException.getMessage must include(
          "No configuration setting found for key 'executionContext'"
        )
      }

      "reject creating a producer with ec class that is not a descendant of ExecutionContext" in {
        val ecClass = this.getClass.getName
        val thrownException = testMisConfiguration(Map("EXECUTION_CONTEXT" -> ecClass))
        thrownException.getClass mustEqual classOf[IllegalArgumentException]
        thrownException.getMessage must include(
          s"$ecClass is not of the same type or a descendant of " +
            s"${classOf[ExecutionContext].getName}"
        )
      }

      "reject creating a producer with specified ec class that doesn't exist" in {
        val thrownException = testMisConfiguration(Map("EXECUTION_CONTEXT" -> "nuggets"))
        thrownException.getClass mustEqual classOf[ClassNotFoundException]
      }
    }
  }

  "findClassInstance" should {
    "correctly find a class in the application runtime" in {
      val mockClass = mock[AvroProducer]
      val mockInjector = mock[Injector]
      val mockApplication = mock[Application]
      val mockClassLoader = mock[ClassLoader]
      val mockLookupService = mock[RuntimeDependencyLookupService]

      when(mockApplication.injector).thenReturn(mockInjector)
      doReturn(mockLookupService).when(mockInjector).instanceOf[RuntimeDependencyLookupService]
      when(mockLookupService.lookup[AvroProducer](any[String](), any[ClassLoader]()))
        .thenReturn(mockClass)
      when(mockApplication.classloader).thenReturn(mockClassLoader)

      val configuration = Configuration.empty
      val className = "chipmunks"

      val spyInitModule = spy(new AvroProducerInitModuleStub("baseConfKey", configuration))
      spyInitModule
        .findClassInstance[AvroProducer](className, () => mockApplication) mustEqual mockClass
      verify(mockApplication).injector
      verify(mockInjector).instanceOf[RuntimeDependencyLookupService]
      verify(mockLookupService).lookup[AvroProducer](className, mockClassLoader)

      // Never directly call the `applicationProvider` method
      verify(spyInitModule, never()).applicationProvider
    }
  }

  "bindProducer" should {
    "correctly bind a producer" in {
      val producerName = "validProduceXyzLm"
      val baseConfigKey = "kafka.injectable.producer.avro"
      val configuration = Configuration.empty
      val spyInitModule = spy(new AvroProducerInitModuleStub(baseConfigKey, configuration))

      doReturn(mockProducer)
        .when(spyInitModule)
        .instantiateProducer(any[Configuration](), any[Provider[Application]]())
      when(mockProducerBinding.annotatedWith(any[NamedKafkaProducerImpl]()))
        .thenReturn(mockProducerBinding)
      doNothing().when(mockProducerBinding).toInstance(any[AvroProducer]())

      val producerConfiguration = Configuration(("sampleKey", "sampleValue"))
      val mockApplicationProvider = mock[Provider[Application]]

      spyInitModule.bindProducer(
        producerName,
        producerConfiguration,
        mockApplicationProvider,
        mockProducerBinding
      )
      verify(mockProducerBinding).annotatedWith(new NamedKafkaProducerImpl(producerName))
      verify(mockProducerBinding).toInstance(mockProducer)
      verify(spyInitModule).instantiateProducer(producerConfiguration, mockApplicationProvider)

      // Never directly call the `producerBinding` and `applicationProvider` methods
      verify(spyInitModule, never()).producerBinding
      verify(spyInitModule, never()).applicationProvider
    }
  }

}
