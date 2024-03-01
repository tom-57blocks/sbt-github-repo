package xyz.jia.scala.commons.messaging.kafka

import scala.annotation.nowarn
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import akka.Done
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.{Logger, LoggerFactory, Marker}

trait MessageProcessor[K, V] {

  val maxMessageProcessingAttempts: Int = 3

  val processorName: String = this.getClass.getName.stripSuffix("$")

  val processorLogger: Logger = LoggerFactory.getLogger(processorName)

  def processMessage(consumerRecord: ConsumerRecord[K, V]): Future[Done]

  /** Overrider to customize how to log the details of a given Kafka record
    * @param consumerRecord
    *   the record to be logged
    * @return
    *   the marker to use to log the record details
    */
  def consumerRecordMarker(@nowarn consumerRecord: ConsumerRecord[K, V]): Marker =
    new BasicMarkerFactory().getMarker(processorName)

  /** Override to offer customer handling for handling records that couldn't be processed using the
    * normal flow implemented in method `processMessage`. An example of when to override this is
    * forward the unprocessable messages to a DLQ.
    * @param consumerRecord
    *   the raw record
    * @param exception
    *   the exception that was thrown when processing the message
    * @return
    *   Done
    */
  def handleUnprocessableMessage(
      consumerRecord: ConsumerRecord[K, V],
      exception: Throwable
  ): Future[Done] = {
    processorLogger.warn(
      consumerRecordMarker(consumerRecord),
      s"Unprocessable Kafka record detected and skipped in $processorName",
      exception
    )
    Future.successful(Done)
  }

  /** Gets the number of times a checksum associated with a Kafka message has been processed. If
    * there has been no record of processing for the given checksum, you are expected to initialize
    * tracking of the processing of the provided checksum.
    *
    * This functionality is normally used for tracking message processing retries.
    *
    * @param messageChecksum
    *   the checksum of the Kafka message
    * @param cacheTtl
    *   the maximum time to live for tracking processing of the given Kafka message checksum
    * @param defaultAttempts
    *   the default value to initialize tracking of checksum with
    * @return
    *   the number of times a given Kafka message checksum has been processed
    */
  def getOrInitProcessingAttempts(
      messageChecksum: String,
      cacheTtl: Duration,
      defaultAttempts: Int
  ): Future[Int]

  def incrementProcessingAttempts(messageChecksum: String, cacheTtl: Duration): Future[Int]

}
