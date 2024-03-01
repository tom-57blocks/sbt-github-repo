package xyz.jia.scala.commons.playutils.tasks

import scala.concurrent.Future

import akka.Done

import xyz.jia.scala.commons.playutils.tasks.TaskErrorCode.TaskErrorCode
import xyz.jia.scala.commons.utils.ErrorMessage

class DummyTaskStorageService extends HttpInvocableTaskRequestRepo {

  override def saveRequest(
      request: HttpInvocableTaskRequest
  ): Future[Either[(Seq[ErrorMessage], TaskErrorCode), Done]] = ???

  override def handleFailedProcessing(
      request: HttpInvocableTaskRequest,
      exception: Throwable
  ): Future[Done] = ???

  override def handleSuccessfulProcessing(request: HttpInvocableTaskRequest): Future[Done] = ???

}
