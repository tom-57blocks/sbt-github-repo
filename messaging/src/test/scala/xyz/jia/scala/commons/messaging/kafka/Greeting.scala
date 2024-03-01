package xyz.jia.scala.commons.messaging.kafka

import com.sksamuel.avro4s.RecordFormat

/** Currently used for testing Avro serialization and publishing of such content to Kafka */
case class Greeting(message: String, recipient: String)

object Greeting {

  val avroRecordFormat: RecordFormat[Greeting] = RecordFormat[Greeting]

}
