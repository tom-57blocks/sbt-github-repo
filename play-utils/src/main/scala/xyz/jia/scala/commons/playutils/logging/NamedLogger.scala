package xyz.jia.scala.commons.playutils.logging

import scala.util.Try

import com.typesafe.config.{Config, ConfigRenderOptions}
import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers.{append, appendRaw}
import play.api.Logging
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.Request

import xyz.jia.scala.commons.playutils.json.{JsonMaskConfig, JsonMaskUtil}

trait NamedLogger extends Logging {

  def httpRequestAppender(
      request: Request[JsValue]
  )(implicit maskConfig: JsonMaskConfig): LogstashMarker = {
    val maskedBody: JsValue =
      Try(JsonMaskUtil.maskSensitiveValues(request.body))
        .getOrElse(JsString("Unable to mask request body"))

    val maskedQueryParameters =
      Try(JsonMaskUtil.maskSensitiveValues(Json.toJson(request.queryString)))
        .getOrElse(JsString("Unable to mask request query parameters"))

    val maskedHeaders =
      Try(JsonMaskUtil.maskSensitiveValues(Json.toJson(request.headers.toMap)))
        .getOrElse(JsString("Unable to mask request headers"))

    val json: JsValue = Json.obj(
      "path" -> request.path,
      "queryParameters" -> maskedQueryParameters,
      "body" -> maskedBody,
      "headers" -> maskedHeaders
    )
    appendRaw("request", json.toString)
  }

  def correlationIdAppender(implicit correlationId: String): LogstashMarker =
    append("correlationId", correlationId)

  def rawJsonAppender(
      key: String,
      json: JsValue
  )(implicit maskConfig: JsonMaskConfig): LogstashMarker = {
    val maskedJson: JsValue =
      Try(JsonMaskUtil.maskSensitiveValues(json)).getOrElse(JsString("Unable to mask content"))

    appendRaw(key, maskedJson.toString)
  }

  def configAppender(
      config: Config,
      loggerField: String = "config"
  )(implicit maskConfig: JsonMaskConfig): LogstashMarker = {
    val maskedConfig: JsValue =
      Try(Json.parse(config.root().render(ConfigRenderOptions.concise())))
        .flatMap(json => Try(JsonMaskUtil.maskSensitiveValues(json)))
        .getOrElse(JsString(s"Unable to mask $loggerField"))

    appendRaw(loggerField, maskedConfig.toString())
  }

}

object NamedLogger extends NamedLogger
