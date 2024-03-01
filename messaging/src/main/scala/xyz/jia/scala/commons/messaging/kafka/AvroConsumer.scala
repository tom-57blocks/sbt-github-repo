package xyz.jia.scala.commons.messaging.kafka

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.DurationConverters.JavaDurationOps

import akka.actor.ActorSystem
import akka.stream.RestartSettings
import com.typesafe.config.Config
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}

import xyz.jia.scala.commons.messaging.kafka.ConsumerConfigKeys.RestartSettings._
import xyz.jia.scala.commons.messaging.kafka.ConsumerConfigKeys._
import xyz.jia.scala.commons.utils.TryOrElse.tryOrElse

class AvroConsumer(
    override val consumerName: String,
    override val consumerConfig: Config,
    actorSystemProvider: => ActorSystem,
    executionContextProvider: => ExecutionContext,
    messageProcessorProvider: => AvroMessageProcessor
) extends GenericConsumer[String, SpecificRecord]
    with AvroSerdeConfigHelper {

  override val topics: Seq[String] = consumerConfig.getStringList(topicsKey).asScala.toList

  override val processingParallelism: Int =
    tryOrElse(consumerConfig.getInt(processingParallelismKey), 1)

  override val restartSettings: RestartSettings =
    RestartSettings(
      minBackoff = tryOrElse(consumerConfig.getDuration(minBackoffKey).toScala, 10.seconds),
      maxBackoff = tryOrElse(consumerConfig.getDuration(maxBackoffKey).toScala, 30.seconds),
      randomFactor = tryOrElse(consumerConfig.getDouble(randomFactorKey), 0.2)
    )

  override val committerSettings: Config = consumerConfig.getConfig(committerSettingsKey)

  override type MessageProcessorType = AvroMessageProcessor

  override def getActorSystem: ActorSystem = actorSystemProvider

  override def getExecutionContext: ExecutionContext = executionContextProvider

  override def getMessageProcessor: AvroMessageProcessor = messageProcessorProvider

  override def keyDeserializer: Deserializer[String] = new StringDeserializer

  override def valueDeserializer: Deserializer[SpecificRecord] = {
    val valueDeserializer = new KafkaAvroDeserializer()
    valueDeserializer.configure(generateSerDeConfig(consumerConfig), false)
    valueDeserializer.asInstanceOf[Deserializer[SpecificRecord]]
  }

}
