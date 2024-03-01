package xyz.jia.scala.commons.messaging.kafka

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer.{Control, NoopControl}
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.RestartSettings
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import com.typesafe.config.Config
import org.apache.commons.codec.digest.DigestUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger

trait GenericConsumer[K, V] {

  val topics: Seq[String]

  val consumerName: String

  val processingParallelism: Int

  val consumerConfig: Config

  val committerSettings: Config

  val restartSettings: RestartSettings

  type MessageProcessorType <: MessageProcessor[K, V]

  def keyDeserializer: Deserializer[K]

  def valueDeserializer: Deserializer[V]

  def getMessageProcessor: MessageProcessorType

  def getActorSystem: ActorSystem

  def getExecutionContext: ExecutionContext

  implicit private lazy val actorSystem: ActorSystem = getActorSystem

  private lazy val executionContext: ExecutionContext = getExecutionContext

  private lazy val messageProcessor: MessageProcessorType = getMessageProcessor

  /** Can be used to stop the stream in a controlled manner. */
  val consumerControl: AtomicReference[Control] = new AtomicReference[Control](NoopControl)

  private lazy val logger: Logger = messageProcessor.processorLogger

  def consumerSettings: ConsumerSettings[K, V] =
    ConsumerSettings(consumerConfig, keyDeserializer, valueDeserializer)

  def generateConsumer: Source[Done, Unit] = {
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topics: _*))
      .mapMaterializedValue(consumerControl.set)
      .mapAsync(processingParallelism) { message: CommittableMessage[K, V] =>
        processMessage(message).map(_ => message.committableOffset)(executionContext)
      }
      .via(Committer.flow(CommitterSettings(committerSettings)))
  }

  def processMessage(message: CommittableMessage[K, V]): Future[Done] = {
    def handleException(exception: Throwable, record: ConsumerRecord[K, V]): Future[Done] = {
      if (exception.getClass.getPackage.getName != "org.apache.kafka.common.errors") {
        // only retry non Kafka related exceptions
        retry(exception, record, messageProcessor.handleUnprocessableMessage)
      } else {
        Future.failed(exception)
      }
    }

    Try(messageProcessor.processMessage(message.record)) match {
      case Success(messageProcessing: Future[Done]) =>
        messageProcessing.recoverWith { case ex =>
          handleException(ex, message.record)
        }(executionContext)
      case Failure(exception) =>
        handleException(exception, message.record)
    }
  }

  def retry(
      throwable: Throwable,
      consumerRecord: ConsumerRecord[K, V],
      failedRecordProcessor: (ConsumerRecord[K, V], Throwable) => Future[Done]
  ): Future[Done] = {
    implicit val ec: ExecutionContext = executionContext
    val maxRecordProcessingAttempts = messageProcessor.maxMessageProcessingAttempts

    val cacheKey: String = DigestUtils.sha1Hex(consumerRecord.toString)
    val cacheTtl: Duration =
      2 * restartSettings.maxBackoff * // Twice max possible restart delay to fact in processing time
        (1 + restartSettings.randomFactor) *
        maxRecordProcessingAttempts.toDouble // Ensure entry is not rest in between retries

    messageProcessor.getOrInitProcessingAttempts(cacheKey, cacheTtl, 1).flatMap { attempts =>
      val displayAttempts = s"ProcessingAttempts => $attempts/$maxRecordProcessingAttempts"
      if (attempts < maxRecordProcessingAttempts) {
        logger.warn(
          messageProcessor.consumerRecordMarker(consumerRecord),
          s"Retrying Kafka message processing | $displayAttempts",
          throwable
        )
        messageProcessor
          .incrementProcessingAttempts(cacheKey, cacheTtl)
          .flatMap(_ => Future.failed(throwable))
      } else {
        logger.warn(
          messageProcessor.consumerRecordMarker(consumerRecord),
          s"Retry attempts for Kafka message processing exhausted | $displayAttempts",
          throwable
        )
        messageProcessor
          .incrementProcessingAttempts(cacheKey, cacheTtl)
          .flatMap(_ => failedRecordProcessor(consumerRecord, throwable))
      }
    }
  }

  /** The consumer is lazily initialized to avoid streaming messages before the application is fully
    * ready
    */
  lazy val stream: Future[Done] =
    RestartSource
      .withBackoff(restartSettings)(() => generateConsumer)
      .runWith(Sink.ignore)

  def startStreaming(): Unit = synchronized {
    // Access the `stream` lazy val object so that it is initialized to initiate streaming
    stream
    logger.info(s"Kafka stream started: $consumerName")
  }

}
