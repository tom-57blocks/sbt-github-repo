package xyz.jia.scala.commons.playutils

import java.io.FileNotFoundException
import java.nio.file.Paths

import scala.util.Try

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class FileUtilsSpec extends AnyWordSpec with Matchers {

  "getFileAsJson" should {
    "correctly parse the contents of a file as JSON" in {
      val expectedContent =
        """
          |[
          |  {
          |    "name": "cat",
          |    "classification": "feline"
          |  },
          |  {
          |    "name": "dog",
          |    "classification": "canine"
          |  }
          |]
          |""".stripMargin
      val path = "play-utils/src/test/resources/fileUtils/animals.json"
      withClue("when given a file path") {
        FileUtils.getFileAsJson(path).get mustEqual Json.parse(expectedContent)
      }

      withClue("when given the file object") {
        val readJson = FileUtils.getFileAsJson(Paths.get(path).toFile).get
        readJson mustEqual Json.parse(expectedContent)
      }
    }

    "correctly handle an invalid file" in {
      val result: Try[JsValue] = FileUtils.getFileAsJson("play-utils/src/test/resources/fileUtils")
      result.isFailure mustBe true
      result.toEither.swap.toOption.get.getClass mustEqual classOf[IllegalArgumentException]
    }
  }

  "getResourceAsJson" should {
    "correctly parse the contents of a file as JSON" in {
      val expectedContent =
        """
          |[
          |  {
          |    "name": "cat",
          |    "classification": "feline"
          |  },
          |  {
          |    "name": "dog",
          |    "classification": "canine"
          |  }
          |]
          |""".stripMargin
      val readJson = FileUtils.getResourceAsJson("fileUtils/animals.json").get
      readJson mustEqual Json.parse(expectedContent)
    }

    "correctly handle an invalid file" in {
      val result = FileUtils.getResourceAsJson("play-utils/src/test/resources/fileUtils")
      result.isFailure mustBe true
      result.toEither.swap.toOption.get.getClass mustEqual classOf[FileNotFoundException]
    }
  }

}
