package xyz.jia.scala.commons.playutils.tasks

case class HttpInvocableTaskConfig(taskName: String, implementation: String)

object HttpInvocableTaskConfig {

  object ConfigKeys {

    val taskKey: String = "task"

    val tasksKey: String = "tasks"

    val baseConfigKey: String = "httpInvocableTasks"

    val requestsRepoKey: String = "requestsRepo"

    val processorKey: String = "processor"

  }

}
