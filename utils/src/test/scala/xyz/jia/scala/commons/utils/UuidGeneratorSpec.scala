package xyz.jia.scala.commons.utils

import java.util.UUID

import scala.util.Try

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UuidGeneratorSpec extends AnyWordSpec with Matchers {

  "generateUuid" should {
    "correctly generate a UUID" in {
      Try(UUID.fromString(UuidGenerator.generateUuid)).toOption must not be empty
    }
  }

}
