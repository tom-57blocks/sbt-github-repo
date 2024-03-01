package xyz.jia.scala.commons.playutils.http.rest

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.DurationConverters.JavaDurationOps

import com.typesafe.config.Config
import play.api.ConfigLoader

case class RestCircuitConfig(
    maxFailures: Int,
    callTimeout: FiniteDuration,
    resetTimeout: FiniteDuration,
    successStatusRestrictions: Set[Int] = Set.empty
) {

  def isSupportedSuccessCode(code: Int): Boolean =
    successStatusRestrictions.isEmpty || successStatusRestrictions.contains(code)

}

object RestCircuitConfig {

  implicit val configLoader: ConfigLoader[RestCircuitConfig] = (rootConfig: Config, path: String) =>
    {
      def parseConfig(configuration: Config): RestCircuitConfig = RestCircuitConfig(
        maxFailures = configuration.getInt("max-failures"),
        callTimeout = configuration.getDuration("call-timeout").toScala,
        resetTimeout = configuration.getDuration("reset-timeout").toScala,
        successStatusRestrictions =
          configuration.getIntList("success-status-restrictions").asScala.map(_.toInt).toSet
      )

      parseConfig(rootConfig.getConfig(path))
    }

}
