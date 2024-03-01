package xyz.jia.scala.commons.messaging.kafka

import java.util

import scala.jdk.CollectionConverters.MapHasAsJava

import com.typesafe.config.Config
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG

trait AvroSerdeConfigHelper {

  def generateSerDeConfig(config: Config): util.Map[String, Any] = Map[String, Any](
    SCHEMA_REGISTRY_URL_CONFIG -> config.getString("kafka-clients.schema.registry.url"),
    SPECIFIC_AVRO_READER_CONFIG -> config.getBoolean("kafka-clients.specific.avro.reader")
  ).asJava

}
