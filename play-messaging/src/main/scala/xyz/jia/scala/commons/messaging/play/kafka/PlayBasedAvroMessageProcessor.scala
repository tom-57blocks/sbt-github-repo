package xyz.jia.scala.commons.messaging.play.kafka

import scala.util.Try

import net.logstash.logback.marker.Markers.appendRaw
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Marker
import play.api.libs.json.{JsString, JsValue, Json}

import xyz.jia.scala.commons.messaging.kafka.AvroMessageProcessor

trait PlayBasedAvroMessageProcessor
    extends PlayBasedMessageProcessor[String, SpecificRecord]
    with AvroMessageProcessor {

  override def consumerRecordMarker(
      consumerRecord: ConsumerRecord[String, SpecificRecord]
  ): Marker = {
    val messageBody = consumerRecord.value().toString
    val jsonMessageBody: JsValue = Try(Json.parse(messageBody)).getOrElse(JsString(messageBody))
    appendRaw("recordDetails", Json.obj("messageBody" -> jsonMessageBody).toString)
  }

}
