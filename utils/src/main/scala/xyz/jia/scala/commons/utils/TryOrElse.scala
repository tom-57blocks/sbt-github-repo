package xyz.jia.scala.commons.utils

import scala.util.Try

object TryOrElse {

  def tryOrElse[T](operation: => T, default: T): T = Try(operation).getOrElse(default)

}
