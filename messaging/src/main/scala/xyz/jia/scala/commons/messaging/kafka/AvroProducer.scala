package xyz.jia.scala.commons.messaging.kafka

import java.util

import scala.concurrent.ExecutionContext

import akka.kafka.ProducerSettings
import com.typesafe.config.Config
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serializer

/** A producer for publishing Avro serialized message payloads to Kafka.
  *
  * The message keys are expected to be in plaintext.
  * @param producerConfig
  *   The configuration to use when setting up the producer. All configuration specified in <a
  *   href="https://doc.akka.io/docs/alpakka-kafka/current/producer.html#settings">Alpakka Kafka
  *   producer settings</a> are supported
  * @param executionContextProvider
  *   The provider of the execution context to use for handling asynchronous operations when
  *   publishing records to Kafka
  */
class AvroProducer(
    val producerConfig: Config,
    executionContextProvider: => ExecutionContext
) extends GenericProducer[String, SpecificRecord]
    with AvroSerdeConfigHelper {

  override def executionContext: ExecutionContext = executionContextProvider

  private val serdeConfig: util.Map[String, Any] = generateSerDeConfig(producerConfig)

  private val valueSerializer: KafkaAvroSerializer = new KafkaAvroSerializer()

  valueSerializer.configure(serdeConfig, false)

  private val keySerializer: KafkaAvroSerializer = new KafkaAvroSerializer()

  keySerializer.configure(serdeConfig, true)

  override val producerSettings: ProducerSettings[String, SpecificRecord] = ProducerSettings(
    producerConfig,
    keySerializer.asInstanceOf[Serializer[String]],
    valueSerializer.asInstanceOf[Serializer[SpecificRecord]]
  )

}
