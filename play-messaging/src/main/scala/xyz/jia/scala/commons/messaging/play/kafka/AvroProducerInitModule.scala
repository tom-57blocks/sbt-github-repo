package xyz.jia.scala.commons.messaging.play.kafka

import scala.concurrent.ExecutionContext

import com.google.inject.Provider
import com.google.inject.binder.AnnotatedBindingBuilder
import org.apache.avro.specific.SpecificRecord
import play.api.{Application, Configuration, Environment}
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.messaging.kafka.AvroProducer

/** Initializes a Kafka Avro producer from application configuration. Below is sample configuration
  * {{{
  *
  * kafka.injectable.producer.avro.quickStart: \${akka.kafka.producer}{
  *   executionContext: "xyz.jia.scala.commons.play.DummyTestExecutionContext"
  *   kafka-clients {
  *     client.id: "sample-messages-producer"
  *     bootstrap.servers: "localhost:19092"
  *     schema.registry.url: "localhost:8081
  *     specific.avro.reader: false
  *   }
  * }
  *
  * }}}
  *
  * Once configured, the Kafka Avro producer can be injected as shown in th example below
  * {{{
  *   @Singleton
  *   class EventPublisher @Inject() (
  *       @NamedKafkaProducer("quickStart") producer: AvroProducer
  *   ) { ...
  * }}}
  *
  * Below are the main takeaways from the configuration format;
  *   - `kafka.injectable.producer.avro` is the keyword that tells the library to initialize a Kafka
  *     Avro producer.
  *   - `quickStart` is the name of the producer. This will also be used when injecting the
  *     producer.
  *   - `executionContext` is the name of an implementation of the execution context to use for
  *     handling asynchronous operations when publishing records to Kafka.
  *
  * The configuration supports provision of all the supported Kafka configuration specified at <a
  * href="https://doc.akka.io/docs/alpakka-kafka/current/producer.html#settings">alpakka-kafka</a>
  *
  * @param environment
  *   The Application environment
  * @param configuration
  *   A full configuration set for the application
  */
class AvroProducerInitModule(
    @annotation.nowarn environment: Environment,
    override val configuration: Configuration
) extends ProducerInitModule[String, SpecificRecord, AvroProducer] {

  override val baseConfigKey = "kafka.injectable.producer.avro"

  override val logMaskConfig: JsonMaskConfig =
    configuration.get[JsonMaskConfig]("default.logging.maskConfig")

  override def instantiateProducer(
      config: Configuration,
      applicationProvider: Provider[Application]
  ): AvroProducer = {
    val executionContextName = config.get[String]("executionContext")
    new AvroProducer(
      config.underlying,
      findClassInstance[ExecutionContext](executionContextName, applicationProvider)
    )
  }

  /** Exposed to facilitate testing. This method is only expected to be called in the `configure`
    * method.
    */
  override def producerBinding: AnnotatedBindingBuilder[AvroProducer] =
    bind(classOf[AvroProducer])

  /** Exposed to facilitate testing. This method is only expected to be called in the `configure`
    * method.
    */
  override def applicationProvider: Provider[Application] =
    getProvider(classOf[Application])

}
