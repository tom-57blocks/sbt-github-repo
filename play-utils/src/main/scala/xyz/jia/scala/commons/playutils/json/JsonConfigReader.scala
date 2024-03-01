package xyz.jia.scala.commons.playutils.json

import java.nio.file.Path

import scala.util.{Failure, Success}

import play.api.libs.json.{JsError, Reads}

import xyz.jia.scala.commons.playutils.ErrorMessageHelpers.JsErrorAsErrorMessage
import xyz.jia.scala.commons.playutils.FileUtils
import xyz.jia.scala.commons.utils.ErrorMessage

/** Contains utility methods that help with reading application configuration that has been provided
  * in JSON format in a specific location
  *
  * @tparam T
  *   The type of configuration being read
  */
trait JsonConfigReader[T] {

  /** The number of nested directories to traverse when reading configuration */
  protected val maxConfigReadDepth: Int = 2

  protected val fileReader: FileUtils

  implicit protected def configJsonReads: Reads[T]

  def loadConfigurations(directory: String): (Seq[ErrorMessage], Seq[T]) = {
    fileReader
      .getFiles(directory, maxConfigReadDepth)
      .map(readConfigurationFile)
      .foldLeft((Seq.empty[ErrorMessage], Seq.empty[T])) {
        case ((accumulatedErrors, accumulatedConfigs), current) =>
          current match {
            case Left(errors)         => (accumulatedErrors ++ errors, accumulatedConfigs)
            case Right(configuration) => (accumulatedErrors, configuration +: accumulatedConfigs)
          }
      }
  }

  def readConfigurationFile(filePath: Path): Either[Seq[ErrorMessage], T] = {
    fileReader.getFileAsJson(filePath.toFile) match {
      case Success(json) =>
        json.validate[T].asEither match {
          case Left(errors)           => Left(JsError(errors).errorMessages)
          case Right(treatmentConfig) => Right(treatmentConfig)
        }
      case Failure(throwable) =>
        val message = s"Invalid JSON detected in configuration file: ${filePath.toString}. " +
          s"Error => ${throwable.getMessage}"
        Left(Seq(ErrorMessage(message)))
    }
  }

}
