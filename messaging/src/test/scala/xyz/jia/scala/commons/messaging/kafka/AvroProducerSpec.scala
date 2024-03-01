package xyz.jia.scala.commons.messaging.kafka

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.mockito.Mockito.spy
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

class AvroProducerSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit val patience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val producerConfig: Config =
    ConfigFactory
      .parseResources("kafka/avroProducer.conf")
      .resolve()
      .getConfig("producer")

  val testTopic: String = producerConfig.getString("avroTestTopic")

  var spyProducer: AvroProducer = spy(new AvroProducer(producerConfig, global))

  override def afterAll(): Unit = {
    super.afterAll()
    // Shutdown the producer before proceeding to next tests
    Await.result(spyProducer.close(), 10.seconds)
  }

  "An instance of the AvroProducer" should {
    "successfully publish messages to Kafka" in {
      val message = Greeting.avroRecordFormat.to(Greeting("Hello", "Bob")): SpecificRecord
      val record = new ProducerRecord(testTopic, "sample-key", message)
      whenReady(spyProducer.publishMessage(record)) { result =>
        result.topic() mustEqual testTopic
        result.offset() >= 0 mustBe true
      }
    }
  }

}
