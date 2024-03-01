package xyz.jia.scala.commons.playutils.tasks

import scala.concurrent.Future

import akka.Done

import xyz.jia.scala.commons.playutils.tasks.TaskErrorCode.TaskErrorCode
import xyz.jia.scala.commons.utils.ErrorMessage

/** An implementation of how and where to store HTTP invocable tasks */
trait HttpInvocableTaskRequestRepo {

  def saveRequest(
      request: HttpInvocableTaskRequest
  ): Future[Either[(Seq[ErrorMessage], TaskErrorCode), Done]]

  def handleFailedProcessing(
      request: HttpInvocableTaskRequest,
      exception: Throwable
  ): Future[Done]

  def handleSuccessfulProcessing(request: HttpInvocableTaskRequest): Future[Done]

}
