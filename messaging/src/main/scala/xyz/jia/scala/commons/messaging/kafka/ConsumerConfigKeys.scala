package xyz.jia.scala.commons.messaging.kafka

object ConsumerConfigKeys {

  val topicsKey: String = "topics"

  val committerSettingsKey: String = "committerSettings"

  val processingParallelismKey: String = "processingParallelism"

  object RestartSettings {

    val minBackoffKey: String = "restart.minBackoff"

    val maxBackoffKey: String = "restart.maxBackoff"

    val randomFactorKey: String = "restart.randomFactor"

  }

}
