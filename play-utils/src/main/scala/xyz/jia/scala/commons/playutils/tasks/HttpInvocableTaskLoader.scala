package xyz.jia.scala.commons.playutils.tasks

import javax.inject.{Inject, Provider, Singleton}

import play.api.{Application, Configuration}

import xyz.jia.scala.commons.playutils.RuntimeDependencyLookupService
import xyz.jia.scala.commons.playutils.logging.NamedLogger

@Singleton
class HttpInvocableTaskLoader @Inject() (
    configuration: Configuration,
    applicationProvider: Provider[Application],
    dependencyLookupService: RuntimeDependencyLookupService
) extends NamedLogger {

  private lazy val application = applicationProvider.get()

  private val tasks: Seq[HttpInvocableTaskConfig] =
    HttpInvocableTaskConfigParser.parseConfiguration(configuration).getOrElse(Nil)

  lazy val requestsRepo: HttpInvocableTaskRequestRepo =
    HttpInvocableTaskConfigParser.parseRequestsRepo(configuration) match {
      case Right(repo) =>
        dependencyLookupService.lookup[HttpInvocableTaskRequestRepo](repo, application.classloader)
      case Left(errors) =>
        logger.error(
          s"Invalid HTTP invocable task configuration provided.\n ${errors.mkString("\n")}"
        )
        throw new IllegalArgumentException("Invalid HTTP invocable task configuration provided")
    }

  def getTask(taskName: String): Option[HttpInvocableTask] = {
    tasks.find(_.taskName == taskName).map { task =>
      dependencyLookupService
        .lookup[HttpInvocableTask](task.implementation, application.classloader)
    }
  }

}
