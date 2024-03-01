package xyz.jia.scala.commons.playutils.json

import java.util.regex.Pattern

import play.api.libs.json._

/** Utility for masking content at various locations in a JSON object */
object JsonMaskUtil {

  /** Masks values at locations specified in the `sensitiveInfoPaths` list. Supports specification
    * of both absolute links and relative links for finding sensitive info. Below is an example;
    * @example
    * {{{
    *     List(
    *       "phoneNumber", // replace all values for key phoneNumber regardless of where the key appears
    *       "/metadata/homeAddress", // Replace the value at absolute path /metadata/homeAddress
    *       "metadata/alternativePhones", Replace the value at absolute path /metadata/alternativePhones
    *     )
    * }}}
    *
    * @param unmaskedJson
    *   the JSON object whose values to mask
    * @param sensitiveInfoPaths
    *   the paths that contain sensitive information
    * @return
    *   The masked JSON
    */
  def maskSensitiveValues(unmaskedJson: JsValue, sensitiveInfoPaths: List[String]): JsValue = {

    def pathUpdater(path: JsPath, json: JsValue): JsValue = {
      val updater = __.read[JsValue].map(_ => JsString("************"))
      json.transform(path.json.update(updater)).getOrElse(json)
    }

    sensitiveInfoPaths
      .map(_.split("/").toList.map(_.trim))
      .foldLeft(unmaskedJson) { (json: JsValue, sensitiveInfoPath: List[String]) =>
        maskJson(json, sensitiveInfoPath, pathUpdater)
      }
  }

  def maskSensitiveValues(unmaskedJson: JsValue)(implicit maskConfig: JsonMaskConfig): JsValue =
    maskSensitiveValues(unmaskedJson, maskConfig.sensitiveInfoPaths)

  @scala.annotation.tailrec
  private def maskJson(
      rawJson: JsValue,
      sensitiveInfoPath: List[String],
      pathUpdater: (JsPath, JsValue) => JsValue
  ): JsValue = {
    (rawJson, sensitiveInfoPath) match {
      case (_, Nil) | (_, List("")) => rawJson
      case (_, List("*"))           => JsString("************")
      case (json, List("", path))   => pathUpdater(__ \ path, json)
      case (json, List(path))       =>
        // Scala JSON doesn't support replacing recursive paths so use regex
        val jsonString = json.toString()
        val matcher = Pattern
          .compile("(" + path + ")\"\\s*:(\\[[^}][^\\]]*\\]|\"[^,\\*]*\"|\\d*)")
          .matcher(jsonString)

        Json.parse {
          if (matcher.find() && matcher.group(1).nonEmpty && matcher.group(2).nonEmpty) {
            matcher.replaceAll("$1\":\"************\"")
          } else {
            jsonString
          }
        }
      case (json, paths) =>
        if (paths.head.trim.isEmpty) {
          maskJson(rawJson, paths.tail, pathUpdater)
        } else {
          pathUpdater(paths.tail.foldLeft(__ \ paths.head)(_ \ _), json)
        }
    }
  }

}
