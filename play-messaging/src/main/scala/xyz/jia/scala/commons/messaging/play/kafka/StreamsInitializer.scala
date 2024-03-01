package xyz.jia.scala.commons.messaging.play.kafka

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.DurationConverters.JavaDurationOps

import akka.actor.{Cancellable, CoordinatedShutdown}
import com.typesafe.config.Config
import play.api.inject.{BindingKey, QualifierInstance}
import play.api.{Application, Configuration}

import xyz.jia.scala.commons.messaging.kafka.GenericConsumer
import xyz.jia.scala.commons.messaging.play.NamedKafkaConsumerImpl
import xyz.jia.scala.commons.messaging.play.kafka.ConsumerInitModule.StandardConfigKeys._
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.playutils.logging.NamedLogger
import xyz.jia.scala.commons.utils.TryOrElse.tryOrElse

trait StreamsInitializer[K, V] extends NamedLogger {

  implicit def executionContext: ExecutionContext

  def consumerClass: Class[_ <: GenericConsumer[K, V]]

  def baseConfigKey: String

  def configuration: Configuration

  implicit val logMaskConfig: JsonMaskConfig =
    configuration.get[JsonMaskConfig]("default.logging.maskConfig")

  configuration.getOptional[Configuration](baseConfigKey).foreach { baseConfig =>
    baseConfig.subKeys.foreach { name =>
      initializeStream(name, baseConfig.get[Configuration](name).underlying)
    }
  }

  def initializeStream(name: String, config: Config): Cancellable = {
    application.actorSystem.scheduler
      .scheduleOnce(delay = tryOrElse(config.getDuration(streamInitDelayKey).toScala, 10.seconds)) {
        logger.info(s"Initializing Kafka stream: $name")(configAppender(config))
        val consumer =
          application.injector.instanceOf(
            BindingKey(consumerClass, Some(QualifierInstance(new NamedKafkaConsumerImpl(name))))
          )

        // Start streaming of messages from the Kafka topic(s)
        consumer.startStreaming()

        application.coordinatedShutdown
          .addTask(CoordinatedShutdown.PhaseServiceUnbind, s"stop-kafka-consumer-$name") { () =>
            logger.info(s"Gracefully shutting down Kafka consumer: $name")
            consumer.consumerControl.get().shutdown()
          }
      }
  }

  def application: Application

}
