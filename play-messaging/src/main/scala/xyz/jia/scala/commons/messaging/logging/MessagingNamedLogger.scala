package xyz.jia.scala.commons.messaging.logging

import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers.appendRaw
import org.apache.kafka.clients.producer.RecordMetadata
import play.api.libs.json.{JsValue, Json}

import xyz.jia.scala.commons.playutils.logging.NamedLogger

trait MessagingNamedLogger extends NamedLogger {

  def kafkaRecordMetadataAppender(
      data: RecordMetadata,
      messageKey: String = "kafkaMessage"
  ): LogstashMarker = {
    val json: JsValue = Json.obj(
      "partition" -> data.partition(),
      "offset" -> data.offset(),
      "topic" -> data.topic()
    )
    appendRaw(messageKey, json.toString)
  }

}
