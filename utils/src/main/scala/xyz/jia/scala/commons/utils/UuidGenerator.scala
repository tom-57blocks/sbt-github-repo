package xyz.jia.scala.commons.utils

import java.util.UUID

trait UuidGenerator {

  def generateUuid: String = UUID.randomUUID().toString

}

object UuidGenerator extends UuidGenerator
