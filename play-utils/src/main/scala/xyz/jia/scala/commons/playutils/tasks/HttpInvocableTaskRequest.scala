package xyz.jia.scala.commons.playutils.tasks

import play.api.libs.json.{JsValue, Json, OFormat}

/** A request to execution a configured HTTP invocable task
  * @param task
  *   the name of the task for execute
  * @param meta
  *   the metadata to provide to the task to be executed
  * @param requestDedupeId
  *   the ID used to uniquely identify the request to execute the task
  */
case class HttpInvocableTaskRequest(
    task: String,
    meta: JsValue,
    requestDedupeId: String
)

object HttpInvocableTaskRequest {

  implicit def jsonFormat: OFormat[HttpInvocableTaskRequest] = Json.format[HttpInvocableTaskRequest]

}
