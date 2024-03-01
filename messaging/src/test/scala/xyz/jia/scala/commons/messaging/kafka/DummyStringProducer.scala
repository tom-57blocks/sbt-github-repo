package xyz.jia.scala.commons.messaging.kafka

import scala.concurrent.ExecutionContext

import akka.kafka.ProducerSettings
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.StringSerializer

class DummyStringProducer(val producerConfig: Config)(implicit ec: ExecutionContext)
    extends GenericProducer[String, String] {

  override val executionContext: ExecutionContext = ec

  override val producerSettings: ProducerSettings[String, String] =
    ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

}
