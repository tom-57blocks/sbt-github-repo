package xyz.jia.scala.commons.messaging.play.kafka

import javax.inject.Inject

import scala.concurrent.ExecutionContext

import org.apache.avro.specific.SpecificRecord
import play.api.{Application, Configuration}

import xyz.jia.scala.commons.messaging.kafka.AvroConsumer

class AvroStreamsInitializer @Inject() (
    app: Application,
    appConfiguration: Configuration
)(implicit ec: ExecutionContext)
    extends StreamsInitializer[String, SpecificRecord] {

  override def baseConfigKey: String = AvroConsumerInitModule.baseConfigKey

  override def consumerClass: Class[AvroConsumer] = classOf[AvroConsumer]

  override def configuration: Configuration = appConfiguration

  override def application: Application = app

  override def executionContext: ExecutionContext = ec

}
