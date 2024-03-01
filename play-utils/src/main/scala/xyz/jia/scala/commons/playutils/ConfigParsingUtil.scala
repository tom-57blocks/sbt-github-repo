package xyz.jia.scala.commons.playutils

import scala.util.{Failure, Success, Try}

import xyz.jia.scala.commons.utils.ErrorMessage

trait ConfigParsingUtil {

  protected def validateKeyType[T](key: String, extractor: => T): Either[ErrorMessage, T] = {
    Try(extractor) match {
      case Success(value) =>
        Right(value)
      case Failure(exception) =>
        val message = s"Invalid or missing config $key. Error details => ${exception.getMessage}"
        Left(ErrorMessage(message))
    }
  }

  protected def validateClassConfig[T](
      key: String,
      extractor: => String,
      clazz: Class[T]
  ): Either[ErrorMessage, String] = {
    validateKeyType(key, extractor) match {
      case Right(className) =>
        val classLoader = Thread.currentThread().getContextClassLoader
        RuntimeDependencyLookupService
          .validateCompatibility(className, classLoader, clazz)
          .map(_ => className)
          .left
          .map(ex => ErrorMessage(s"${ex.getClass.getSimpleName} : ${ex.getMessage}"))
      case other =>
        other
    }
  }

}
