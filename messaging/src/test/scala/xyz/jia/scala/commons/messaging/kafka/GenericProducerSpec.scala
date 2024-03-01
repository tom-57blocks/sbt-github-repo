package xyz.jia.scala.commons.messaging.kafka

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.mockito.Mockito.{spy, verify}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

class GenericProducerSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val patience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val producerConfig: Config =
    ConfigFactory
      .parseResources("kafka/stringProducer.conf")
      .resolve()
      .getConfig("producer")

  val testTopic: String = producerConfig.getString("plainTextTestTopic")

  val spyProducer: DummyStringProducer = spy(new DummyStringProducer(producerConfig))

  override def afterAll(): Unit = {
    super.afterAll()
    // Shutdown the producer before proceeding to next tests
    Await.result(spyProducer.close(), 10.seconds)
  }

  "publishMessage" should {
    "successfully publish a message to Kafka" in {
      val message = new ProducerRecord(testTopic, "cats", "10")
      whenReady(spyProducer.publishMessage(message)) { result: RecordMetadata =>
        result.topic() mustEqual testTopic
        result.offset() >= 0 mustBe true

        // confirm that this method delegates to publishMessages
        verify(spyProducer).publishMessages(message)
      }
    }
  }

  "publishMessages" should {
    "successfully publish messages to Kafka" in {
      whenReady(
        spyProducer.publishMessages(new ProducerRecord(testTopic, "cats", "10"))
      ) { results: Seq[RecordMetadata] =>
        results.map { result =>
          result.topic() mustEqual testTopic
          result.offset() >= 0 mustBe true
        }
      }
    }
  }

}
