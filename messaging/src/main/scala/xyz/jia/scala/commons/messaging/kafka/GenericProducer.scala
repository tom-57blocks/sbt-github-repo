package xyz.jia.scala.commons.messaging.kafka

import scala.concurrent.{ExecutionContext, Future, Promise}

import akka.kafka.ProducerSettings
import org.apache.kafka.clients.producer.{Callback, Producer, ProducerRecord, RecordMetadata}

/** Generic Kafka producer instance.
  *
  * @tparam K
  *   The key type for records published to Kafka
  * @tparam V
  *   The value type for records published to Kafka
  */
trait GenericProducer[K, V] {

  /** The settings to use when creating the Kafka producer */
  val producerSettings: ProducerSettings[K, V]

  /** Execution context to use for handling asynchronous operations when publishing records to Kafka
    */
  def executionContext: ExecutionContext

  private lazy val kafkaProducer: Future[Producer[K, V]] =
    producerSettings.createKafkaProducerAsync()(executionContext)

  /** Publishes a message to Kafka.
    *
    * @param message
    *   The message to publish to Kafka
    * @return
    *   The metadata associated with the published message
    */
  def publishMessage(message: ProducerRecord[K, V]): Future[RecordMetadata] =
    publishMessages(message).map(_.head)(executionContext)

  /** Publishes one or more messages to Kafka.
    *
    * @param messages
    *   The messages to publish to Kafka
    * @return
    *   The metadata associated with each of the published messages
    */
  def publishMessages(messages: ProducerRecord[K, V]*): Future[List[RecordMetadata]] = {
    implicit val ec: ExecutionContext = executionContext

    def callBack(promise: Promise[RecordMetadata]): Callback =
      (metadata: RecordMetadata, exception: Exception) =>
        if (exception == null) promise.success(metadata) else promise.failure(exception)

    kafkaProducer.flatMap { producer =>
      Future.traverse(messages.toList) { record =>
        val promise = Promise[RecordMetadata]()
        producer.send(record, callBack(promise))
        promise.future
      }
    }
  }

  /** Close the producer. */
  def close(): Future[Unit] = {
    implicit val ec: ExecutionContext = executionContext
    kafkaProducer.map(_.close())
  }

}
