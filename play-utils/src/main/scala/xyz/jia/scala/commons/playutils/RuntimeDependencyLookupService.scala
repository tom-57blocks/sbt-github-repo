package xyz.jia.scala.commons.playutils

import javax.inject.{Inject, Singleton}

import scala.util.Try

import akka.Done
import com.google.inject.Provider
import play.api.Application
import play.api.inject.Injector

/** Looks up dependencies from the application.
  *
  * This is especially useful when you for example want to look up a dependency by its class name.
  *
  * @param applicationProvider
  *   The Play! application provider
  */
@Singleton
class RuntimeDependencyLookupService @Inject() (applicationProvider: Provider[Application]) {

  private lazy val injector: Injector = applicationProvider.get().injector

  /** Looks up an injectable class from the application injector.
    *
    * @param className
    *   The name of the class whose implementation to get from the application's injector
    * @param classLoader
    *   The classloader from which to look up the class
    * @tparam T
    *   The expected object instance type to look up
    * @return
    *   The object instance
    */
  def lookup[T](className: String, classLoader: ClassLoader): T =
    injector.instanceOf(Class.forName(className, false, classLoader)).asInstanceOf[T]

}

object RuntimeDependencyLookupService {

  /** Check if the class represented by the `className` exists and is either the same as, or is a
    * descendant of the class or interface represented by the specified Class[T] parameter. It
    * returns Right if so; otherwise it returns Left.
    *
    * @param className
    *   The name of the class to look up
    * @param classLoader
    *   The classloader from which to look up the class
    * @tparam T
    *   the expected type for the provided class name
    * @return
    *   Right if the class exists and is a descendant of `T` else Left with the exception containing
    *   violation details
    */
  def validateCompatibility[T](
      className: String,
      classLoader: ClassLoader,
      clazz: Class[T]
  ): Either[Throwable, Done] = {
    Try {
      if (!clazz.isAssignableFrom(Class.forName(className, false, classLoader)))
        throw new IllegalArgumentException(
          s"$className is not of the same type or a descendant of ${clazz.getName}"
        )
    }.fold((exception: Throwable) => Left(exception), (_: Unit) => Right(Done))
  }

}
