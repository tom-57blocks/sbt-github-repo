package xyz.jia.scala.commons.playutils.tasks

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import akka.Done
import net.logstash.logback.marker.LogstashMarker

import xyz.jia.scala.commons.playutils.logging.NamedLogger
import xyz.jia.scala.commons.playutils.tasks.TaskErrorCode.{TaskErrorCode, ValidationFailed}
import xyz.jia.scala.commons.utils.ErrorMessage

@Singleton
class HttpInvocableTaskService @Inject() (
    taskLoader: HttpInvocableTaskLoader
)(implicit ec: ExecutionContext)
    extends NamedLogger {

  def processTask(
      request: HttpInvocableTaskRequest
  ): Future[Either[(Seq[ErrorMessage], TaskErrorCode), Done]] = {
    val logMarker: LogstashMarker = correlationIdAppender(request.requestDedupeId)
    taskLoader.getTask(request.task) match {
      case Some(task) =>
        task.parseData(request.meta).flatMap {
          case Right(data)  => processTask(request, task, data)
          case Left(errors) => Future.successful(Left((errors, ValidationFailed)))
        }
      case None =>
        logger.warn(s"Unsupported task provided")(logMarker)
        val error = ErrorMessage(s"Could not find requested task ${request.task}")
        Future.successful(Left((Seq(error), ValidationFailed)))
    }
  }

  private def processTask[T <: HttpInvocableTask](
      rawRequest: HttpInvocableTaskRequest,
      task: T,
      taskData: T#DataType
  ): Future[Either[(Seq[ErrorMessage], TaskErrorCode), Done]] = {
    val logMarker: LogstashMarker = correlationIdAppender(rawRequest.requestDedupeId)
    taskLoader.requestsRepo.saveRequest(rawRequest).map {
      case Right(_) =>
        task.process(taskData.asInstanceOf[task.DataType]).onComplete {
          case Failure(exception) =>
            logger.warn(s"Error processing task", exception)(logMarker)
            taskLoader.requestsRepo.handleFailedProcessing(rawRequest, exception)
          case Success(_) =>
            logger.info(s"Successfully processed task")(logMarker)
            taskLoader.requestsRepo.handleSuccessfulProcessing(rawRequest)
        }
        Right(Done)
      case other => other
    }
  }

}
