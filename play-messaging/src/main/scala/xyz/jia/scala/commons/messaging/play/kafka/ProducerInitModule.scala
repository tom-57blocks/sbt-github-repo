package xyz.jia.scala.commons.messaging.play.kafka

import scala.concurrent.ExecutionContext

import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.{AbstractModule, Provider}
import play.api.{Application, Configuration}
import ProducerInitModule.StandardConfigKeys
import xyz.jia.scala.commons.playutils.RuntimeDependencyLookupService
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.playutils.logging.NamedLogger
import xyz.jia.scala.commons.messaging.kafka.GenericProducer
import xyz.jia.scala.commons.messaging.play.NamedKafkaProducerImpl

trait ProducerInitModule[K, V, T <: GenericProducer[K, V]] extends AbstractModule with NamedLogger {

  val baseConfigKey: String

  implicit val logMaskConfig: JsonMaskConfig

  val configuration: Configuration

  /** Creates injectable instances of all configured Kafka producers if valid configuration has been
    * provided for all of them. Throws exceptions that could abort application startup if any
    * producer is mis-configured.
    */
  override def configure(): Unit = {
    configuration.getOptional[Configuration](baseConfigKey).foreach { baseConfig =>
      baseConfig.subKeys.foreach { name =>
        val config: Configuration = baseConfig.get[Configuration](name)
        logger.info(s"Bootstrapping Kafka producer: $name")(configAppender(config.underlying))
        RuntimeDependencyLookupService.validateCompatibility(
          config.get[String](StandardConfigKeys.executionContext),
          Thread.currentThread().getContextClassLoader,
          classOf[ExecutionContext]
        ) match {
          case Left(throwable: Throwable) => throw throwable
          case Right(_) => bindProducer(name, config, applicationProvider, producerBinding)
        }
      }
    }
  }

  /** Exposed to facilitate testing. This method is only expected to be called in the `configure`
    * method.
    */
  def applicationProvider: Provider[Application]

  /** Looks up an instance of the provided `className` from the Application runtime.
    *
    * @param className
    *   The name of the class whose instance to find
    * @param applicationProvider
    *   The application provider
    * @tparam C
    *   The class type
    * @return
    *   An instance `className` found in the application
    */
  def findClassInstance[C](className: String, applicationProvider: Provider[Application]): C = {
    val application: Application = applicationProvider.get()
    application.injector
      .instanceOf[RuntimeDependencyLookupService]
      .lookup[C](className, application.classloader)
  }

  /** Exposed to facilitate testing. This method is only expected to be called in the `configure`
    * method.
    */
  def producerBinding: AnnotatedBindingBuilder[T]

  def instantiateProducer(config: Configuration, applicationProvider: Provider[Application]): T

  def bindProducer(
      name: String,
      configuration: Configuration,
      applicationProvider: Provider[Application],
      producerBinder: AnnotatedBindingBuilder[T]
  ): Unit = {
    producerBinder
      .annotatedWith(new NamedKafkaProducerImpl(name))
      .toInstance(instantiateProducer(configuration, applicationProvider))
  }

}

object ProducerInitModule {

  object StandardConfigKeys {

    val executionContext: String = "executionContext"

  }

}
