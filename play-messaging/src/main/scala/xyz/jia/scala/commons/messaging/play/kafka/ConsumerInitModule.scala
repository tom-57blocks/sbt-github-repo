package xyz.jia.scala.commons.messaging.play.kafka

import scala.concurrent.ExecutionContext

import akka.Done
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.{AbstractModule, Provider}
import play.api.{Application, Configuration}
import ConsumerInitModule.StandardConfigKeys._
import xyz.jia.scala.commons.playutils.RuntimeDependencyLookupService
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.playutils.logging.NamedLogger
import xyz.jia.scala.commons.messaging.kafka.{GenericConsumer, MessageProcessor}
import xyz.jia.scala.commons.messaging.play.NamedKafkaConsumerImpl

trait ConsumerInitModule[K, V, T <: GenericConsumer[K, V]] extends AbstractModule with NamedLogger {

  val logMaskConfig: JsonMaskConfig

  val baseConfigKey: String

  val configuration: Configuration

  val MessageProcessorImpl: Class[_ <: MessageProcessor[K, V]]

  override def configure(): Unit = {
    configuration.getOptional[Configuration](baseConfigKey).foreach { baseConfig =>
      baseConfig.subKeys.foreach { name =>
        val config: Configuration = baseConfig.get[Configuration](name)
        val logAppender = configAppender(config.underlying)(logMaskConfig)
        logger.info(s"Bootstrapping Kafka consumer: $name")(logAppender)
        validateConfig(config) match {
          case Right(_) =>
            bindConsumer(name, config, applicationProvider, consumerBinding)
          case Left(exceptions: Seq[Throwable]) =>
            exceptions.foreach { exception =>
              logger.error(s"Invalid configuration provided for Kafka consumer: $name", exception)
            }
            // throw one exception to abort start up
            throw exceptions.head
        }
      }
      // Ensure that streaming will be initiated automatically after the application has booted up
      streamsInitializer.asEagerSingleton()
    }
  }

  def validateConfig(config: Configuration): Either[List[Throwable], Done] = {
    val ecValidation: Either[Throwable, Done] =
      RuntimeDependencyLookupService.validateCompatibility(
        config.get[String](executionContextKey),
        Thread.currentThread().getContextClassLoader,
        classOf[ExecutionContext]
      )

    val processorValidation: Either[Throwable, Done] =
      RuntimeDependencyLookupService.validateCompatibility(
        config.get[String](messageProcessorKey),
        Thread.currentThread().getContextClassLoader,
        MessageProcessorImpl
      )

    List(
      ecValidation,
      processorValidation
    ).flatMap(_.swap.toOption) match {
      case Nil        => Right(Done)
      case exceptions => Left(exceptions)
    }
  }

  def bindConsumer(
      name: String,
      configuration: Configuration,
      applicationProvider: Provider[Application],
      consumerBinder: AnnotatedBindingBuilder[T]
  ): Unit = {
    consumerBinder
      .annotatedWith(new NamedKafkaConsumerImpl(name))
      .toInstance(instantiateConsumer(name, configuration, applicationProvider))
  }

  def findClassInstance[C](className: String, applicationProvider: Provider[Application]): C = {
    val application: Application = applicationProvider.get()
    application.injector
      .instanceOf[RuntimeDependencyLookupService]
      .lookup[C](className, application.classloader)
  }

  def applicationProvider: Provider[Application]

  def consumerBinding: AnnotatedBindingBuilder[T]

  def instantiateConsumer(
      name: String,
      configuration: Configuration,
      applicationProvider: Provider[Application]
  ): T

  def streamsInitializer: AnnotatedBindingBuilder[_ <: StreamsInitializer[K, V]]

}

object ConsumerInitModule {

  object StandardConfigKeys {

    val executionContextKey: String = "executionContext"

    val messageProcessorKey: String = "messageProcessor"

    val streamInitDelayKey = "streamInitDelay"

  }

}
