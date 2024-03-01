package xyz.jia.scala.commons.messaging.play.kafka

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.google.inject.binder.AnnotatedBindingBuilder
import org.apache.avro.specific.SpecificRecord
import play.api.{Application, Configuration, Environment}
import ConsumerInitModule.StandardConfigKeys._
import xyz.jia.scala.commons.playutils.json.JsonMaskConfig
import xyz.jia.scala.commons.messaging.kafka.{AvroConsumer, AvroMessageProcessor}

/** Initializes a Kafka Avro consumer from application configuration. Below is sample configuration
  * {{{
  *   kafka.injectable.consumer.avro.quickStart: \${akka.kafka.consumer}{
  *     executionContext: "xyz.jia.kafkademo.contexts.DummyTestExecutionContext"
  *     messageProcessor: "xyz.jia.kafkademo.contexts.DummyTestAvroConsumer"
  *     topics: ["quickstart-demo"]
  *     kafka-clients {
  *       client.id: "sample-messages-consumer"
  *       group.id: "sample-messages-consumer"
  *       bootstrap.servers: "kafka-broker0:9092"
  *       schema.registry.url: "http://schema-registry:8081"
  *       specific.avro.reader: false
  *     }
  *
  *     committerSettings: \${akka.kafka.committer} {
  *       max-batch: 10
  *     }
  *   }
  *
  * }}}
  *
  * Once configured, the Kafka Avro consumer can be injected as shown in th example below
  * {{{
  *   @Singleton
  *   class EventPublisher @Inject() (
  *       @NamedKafkaConsumer("quickStart") consumer: AvroConsumer
  *   ) { ...
  * }}}
  *
  * Below are the main takeaways from the configuration format;
  *   - `kafka.injectable.consumer.avro` is the keyword that tells the library to initialize a Kafka
  *     Avro consumer.
  *   - `quickStart` is the name of the consumer. This will also be used when injecting the
  *     consumer.
  *   - `executionContext` is the name of an implementation of the execution context to use for
  *     handling asynchronous operations when processing obtained messages.
  *   - `streamInitDelay` is an optional configuration that controls how long to delay streaming
  *     messages from Kafka after the application has started. The default is 10 seconds
  *   - `processingParallelismKey` is an optional configuration that controls the number of messages
  *     to process in parallel. Set this to 1 if you require strict ordering guarantees. The default
  *     value is 1
  *   - `restart.minBackoff` is an optional configuration that controls the minimum amount of time
  *     streaming should be paused after it is stopped. Default is 10 seconds to ensure that we do
  *     not prematurely restart streaming when the application is trying to shutdown.
  *   - `restart.maxBackoff` is an optional configuration that controls the maximum amount of time
  *     streaming should be paused after it is stopped. The default is 30 seconds.
  *   - `restart.randomFactor` The exponential backoff factor to use when varying restart wait
  *     times. Default is 0.2 which adds up to 20% delay.
  *
  * The configuration supports provision of all the supported Kafka configuration specified at <a
  * href="https://doc.akka.io/docs/alpakka-kafka/current/consumer.html#settings">alpakka-kafka</a>
  *
  * @param environment
  *   The Application environment
  * @param configuration
  *   A full configuration set for the application
  */
class AvroConsumerInitModule(
    @annotation.nowarn environment: Environment,
    override val configuration: Configuration
) extends ConsumerInitModule[String, SpecificRecord, AvroConsumer] {

  override val baseConfigKey: String = AvroConsumerInitModule.baseConfigKey

  override val logMaskConfig: JsonMaskConfig =
    configuration.get[JsonMaskConfig]("default.logging.maskConfig")

  override val MessageProcessorImpl: Class[AvroMessageProcessor] = classOf[AvroMessageProcessor]

  override def instantiateConsumer(
      name: String,
      configuration: Configuration,
      applicationProvider: Provider[Application]
  ): AvroConsumer = {
    val executionContextName = configuration.get[String](executionContextKey)
    val messageProcessorName = configuration.get[String](messageProcessorKey)
    lazy val injector = applicationProvider.get().injector
    new AvroConsumer(
      name,
      configuration.underlying,
      injector.instanceOf(classOf[ActorSystem]), // todo fix this
      findClassInstance[ExecutionContext](executionContextName, applicationProvider),
      findClassInstance[AvroMessageProcessor](messageProcessorName, applicationProvider)
    )
  }

  /** Exposed to facilitate testing. This method is only expected to be called in the `configure`
    * method.
    */
  override def consumerBinding: AnnotatedBindingBuilder[AvroConsumer] =
    bind(classOf[AvroConsumer])

  /** Exposed to facilitate testing. This method is only expected to be called in the `configure`
    * method.
    */
  override def applicationProvider: Provider[Application] =
    getProvider(classOf[Application])

  override def streamsInitializer: AnnotatedBindingBuilder[AvroStreamsInitializer] =
    bind(classOf[AvroStreamsInitializer])

}

object AvroConsumerInitModule {

  val baseConfigKey: String = "kafka.injectable.consumer.avro"

}
