package xyz.jia.scala.commons.playutils.tasks

import scala.annotation.nowarn

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

import xyz.jia.scala.commons.playutils.logging.NamedLogger
import xyz.jia.scala.commons.playutils.tasks.HttpInvocableTaskConfig.ConfigKeys

class HttpInvocableTaskConfigValidationModule(
    @nowarn environment: Environment,
    configuration: Configuration
) extends AbstractModule
    with NamedLogger {

  override def configure(): Unit = {
    configuration.getOptional[Configuration](ConfigKeys.baseConfigKey).foreach { baseConfig =>
      val errors = HttpInvocableTaskConfigParser.validateBaseConfig(baseConfig.underlying)
      if (errors.nonEmpty) {
        logger.error("Invalid Http Invocable Task Configuration provided")
        errors.foreach { error =>
          logger.error(s"Http Invocable Task Configuration error => ${error.message}")
        }
        // throw one exception inorder to abort application start up until configuration is fixed
        throw new IllegalArgumentException(errors.head.message)
      }
    }
  }

}
