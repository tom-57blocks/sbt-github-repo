package xyz.jia.scala.commons.messaging.logging

import java.sql.Timestamp
import java.time.LocalDateTime

import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MessagingNamedLoggerSpec extends AnyWordSpec with Matchers {

  "kafkaRecordMetadataAppender" should {
    "correctly append RecordMetadata details" in {
      val metadata = new RecordMetadata(
        new TopicPartition("dummyTopic", 19),
        2,
        3,
        Timestamp.valueOf(LocalDateTime.now()).getTime,
        122,
        34,
        765
      )

      val appender = new MessagingNamedLogger {}.kafkaRecordMetadataAppender(metadata)
      appender.toString mustEqual
        """kafkaMessage={"partition":19,"offset":5,"topic":"dummyTopic"}"""
    }
  }

}
