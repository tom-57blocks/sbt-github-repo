package xyz.jia.scala.commons.playutils.http

import play.api.libs.json.{JsArray, JsValue, Json, OFormat}

import xyz.jia.scala.commons.playutils.http.ServiceResponsePayload.Metadata

/** Typical Sendy Service HTTP response payload
  *
  * @param data
  *   The data associated with the response
  * @param meta
  *   The metadata associated with the response
  */
case class ServiceResponsePayload(data: JsValue, meta: Metadata)

object ServiceResponsePayload {

  /** Response metadata
    *
    * @param errors
    *   Json array containing all errors to return
    * @param executionTime
    *   The execution time to report for the operation
    * @param requestId
    *   The request ID associated with the response
    * @param resultCode
    *   The code associated with the response
    */
  case class Metadata(
      errors: Option[JsArray],
      executionTime: Long,
      requestId: String,
      resultCode: String
  )

  implicit def metadataFormats: OFormat[Metadata] = Json.format[Metadata]

  implicit def payloadFormats: OFormat[ServiceResponsePayload] = Json.format[ServiceResponsePayload]

}
