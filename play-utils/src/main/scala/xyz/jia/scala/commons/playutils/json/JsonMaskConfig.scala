package xyz.jia.scala.commons.playutils.json

import scala.jdk.CollectionConverters.ListHasAsScala

import com.typesafe.config.Config
import play.api.ConfigLoader

/** Utility for parsing JSON mask configuration from application configuration
  *
  * @param sensitiveInfoPaths
  *   the paths that contain sensitive information
  */
case class JsonMaskConfig(sensitiveInfoPaths: List[String])

object JsonMaskConfig {

  implicit val configLoader: ConfigLoader[JsonMaskConfig] = (rootConfig: Config, path: String) => {
    def parseConfig(configuration: Config): JsonMaskConfig =
      JsonMaskConfig(sensitiveInfoPaths =
        configuration.getStringList("sensitiveInfoPaths").asScala.toList
      )

    parseConfig(rootConfig.getConfig(path))
  }

}
