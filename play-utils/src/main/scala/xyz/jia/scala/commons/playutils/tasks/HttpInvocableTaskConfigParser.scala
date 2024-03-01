package xyz.jia.scala.commons.playutils.tasks

import scala.jdk.CollectionConverters.ListHasAsScala

import com.typesafe.config.Config
import play.api.Configuration

import xyz.jia.scala.commons.playutils.ConfigParsingUtil
import xyz.jia.scala.commons.playutils.tasks.HttpInvocableTaskConfig.ConfigKeys
import xyz.jia.scala.commons.utils.ErrorMessage

trait HttpInvocableTaskConfigParser extends ConfigParsingUtil {

  /** Validates provided HTTP invocable task configuration. Below is a general structure of the
    * configuration;
    * @example
    * {{{
    *     httpInvocableTasks {
    *       requestsRepo: "className"
    *       tasks: [
    *         {
    *           task: "AccountClosing"
    *           processor: "className"
    *         }
    *       ]
    *     }
    * }}}
    *
    * In the above the following is required to be true;
    *
    * <ul>
    *
    * <li>httpInvocableTasks; this is the base configuration container</li>
    *
    * <li>requestsRepo; A fully qualified name of a class that implements
    * `HttpInvocableTaskRequestRepo` </li>
    *
    * <li>tasks; this is the base configuration container for invocable tasks. It's structure is
    * specified below
    *
    * <ul>
    *
    * <li>task; is the name of the task and is a string</li>
    *
    * <li>processor is the fully qualified name of the class that extends `HttpInvocableTask`. This
    * class is expected to contain the logic for processing this task</li>
    *
    * </ul>
    *
    * </li>
    *
    * </ul>
    * @param baseConfig
    *   the specified HTTP invocable task configuration in the application
    * @return
    *   a sequence of errors if any
    */
  def validateBaseConfig(baseConfig: Config): Seq[ErrorMessage] = {
    Seq(
      validateClassConfig(
        ConfigKeys.requestsRepoKey,
        baseConfig.getString(ConfigKeys.requestsRepoKey),
        classOf[HttpInvocableTaskRequestRepo]
      ),
      validateKeyType(ConfigKeys.tasksKey, baseConfig.getConfigList(ConfigKeys.tasksKey))
    ).foldLeft(Seq.empty[ErrorMessage]) { (errors, validation) =>
      validation.swap.toOption.map(errors :+ _).getOrElse(errors)
    } match {
      case Nil =>
        baseConfig.getConfigList(ConfigKeys.tasksKey).asScala.foldLeft(Seq.empty[ErrorMessage]) {
          (errors, taskConfig) =>
            errors :++ parseTaskConfig(taskConfig).swap.toOption.getOrElse(Nil)
        }
      case errors =>
        errors
    }
  }

  def parseRequestsRepo(configuration: Configuration): Either[Seq[ErrorMessage], String] = {
    configuration
      .getOptional[Config](ConfigKeys.baseConfigKey)
      .map { baseConfig =>
        val errors = validateBaseConfig(baseConfig)
        if (errors.nonEmpty) {
          Left(errors)
        } else {
          validateKeyType(
            ConfigKeys.requestsRepoKey,
            baseConfig.getString(ConfigKeys.requestsRepoKey)
          ).left.map(Seq(_))
        }
      }
      .getOrElse(Left(Seq(ErrorMessage("No HTTP Invocable Task Configuration provided"))))
  }

  /** Parses HTTP invocable task configurations (if specified) from the application configuration.
    * For detailed explanation on the format of the expected configuration refer to the method
    * `validateBaseConfig`
    *
    * @param configuration
    *   the application configuration from which the parse the HTTP invocable tasks.
    * @return
    *   either a sequence of errors encountered when parsing the configurations or a sequence of
    *   `HttpInvocableTaskConfig`
    */
  def parseConfiguration(
      configuration: Configuration
  ): Either[Seq[ErrorMessage], Seq[HttpInvocableTaskConfig]] = {
    configuration
      .getOptional[Config](ConfigKeys.baseConfigKey)
      .map { baseConfig =>
        val errors = validateBaseConfig(baseConfig)
        if (errors.nonEmpty) {
          Left(errors)
        } else {
          val (taskParsingErrors, configs) =
            baseConfig
              .getConfigList(ConfigKeys.tasksKey)
              .asScala
              .toSeq
              .map(parseTaskConfig)
              .partition(_.isLeft)

          if (taskParsingErrors.nonEmpty) {
            Left(taskParsingErrors.flatMap(_.swap.toOption).flatten)
          } else {
            Right(configs.flatMap(_.toOption))
          }
        }
      }
      .getOrElse(Right(Nil))
  }

  /** Parses the specified configuration for a given task. Example valid configuration is;
    *
    * @example
    * {{{
    *   {
    *     task: "CloseAccountStatements"
    *     processor: "className"
    *   }
    * }}}
    *
    * In the above the following is required to be true;
    *
    * <ul>
    *
    * <li>task; is the name of the task and is a string</li>
    *
    * <li>processor is the fully qualified name of the class that extends `HttpInvocableTask`. This
    * class is expected to contain the logic for processing this task</li>
    *
    * </ul>
    * @param taskConfig
    *   the extracted configuration for the task to be validated
    * @return
    *   a sequence of errors if any
    */
  def parseTaskConfig(taskConfig: Config): Either[Seq[ErrorMessage], HttpInvocableTaskConfig] = {
    val errors = Seq(
      validateKeyType(ConfigKeys.taskKey, taskConfig.getString(ConfigKeys.taskKey)),
      validateClassConfig(
        ConfigKeys.processorKey,
        taskConfig.getString(ConfigKeys.processorKey),
        classOf[HttpInvocableTask]
      )
    ).foldLeft(Seq.empty[ErrorMessage]) { (errors, validation) =>
      validation.swap.toOption.map(errors :+ _).getOrElse(errors)
    }

    if (errors.isEmpty) {
      Right(
        HttpInvocableTaskConfig(
          taskName = taskConfig.getString(ConfigKeys.taskKey),
          implementation = taskConfig.getString(ConfigKeys.processorKey)
        )
      )
    } else {
      Left(errors)
    }
  }

}

object HttpInvocableTaskConfigParser extends HttpInvocableTaskConfigParser
