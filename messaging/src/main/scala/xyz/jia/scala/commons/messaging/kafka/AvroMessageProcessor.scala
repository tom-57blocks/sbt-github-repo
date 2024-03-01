package xyz.jia.scala.commons.messaging.kafka

import org.apache.avro.specific.SpecificRecord

trait AvroMessageProcessor extends MessageProcessor[String, SpecificRecord]
