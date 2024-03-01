package xyz.jia.scala.commons.utils

import java.io.FileNotFoundException

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FileUtilsSpec extends AnyWordSpec with Matchers {

  "getFile" should {
    "get the path for a file if it exists and it is a regular file" in {
      FileUtils
        .getFile("utils/src/test/resources/fileUtils/animals.json")
        .endsWith("animals.json") mustBe true
    }

    "throw an exception if the specified file doesn't exist" in {
      a[IllegalArgumentException] mustBe thrownBy {
        FileUtils.getFile("utils/src/test/resources/fileUtils/donkeys.json")
      }
    }

    "throw an exception if instead of a file, a directory path is specified" in {
      val file = "utils/src/test/resources/fileUtils"

      (the[IllegalArgumentException] thrownBy {
        FileUtils.getFile(file)
      } must have).message(s"requirement failed: '$file' is not a regular file or doesn't exist")
    }
  }

  "getFileAsString" should {
    "correctly parse the contents of a file as a string" in {
      val expectedContent =
        """plant,classification
          |Maple,Shrub
          |Mint,Herb""".stripMargin

      val path = "utils/src/test/resources/fileUtils/plants.txt"
      FileUtils.getFileAsString(path).get mustEqual expectedContent
    }

    "correctly handle an invalid file" in {
      val result = FileUtils.getFileAsString("utils/src/test/resources/fileUtils")
      result.isFailure mustBe true
      result.toEither.swap.toOption.get.getClass mustEqual classOf[IllegalArgumentException]
    }
  }

  "getResourceAsString" should {
    "correctly parse the contents of a file as a string" in {
      val expectedContent =
        """plant,classification
          |Maple,Shrub
          |Mint,Herb""".stripMargin
      FileUtils.getResourceAsString("fileUtils/plants.txt").get mustEqual expectedContent
    }

    "correctly handle an invalid file" in {
      val result = FileUtils.getResourceAsString("utils/src/test/resources/fileUtils")
      result.isFailure mustBe true
      result.toEither.swap.toOption.get.getClass mustEqual classOf[FileNotFoundException]
    }
  }

  "getFiles" should {
    "read only top level files" in {
      val files =
        FileUtils.getFiles("utils/src/test/resources/fileUtils").map(_.getFileName.toString)

      files.size mustEqual 2
      files must contain allElementsOf List("animals.json", "plants.txt")
    }

    "read files up to one level deep" in {
      val files =
        FileUtils.getFiles("utils/src/test/resources/fileUtils", 2).map(_.getFileName.toString)

      files.size mustEqual 4
      files must contain allElementsOf List(
        "animals.json",
        "plants.txt",
        "animals_a.json",
        "plants_a.txt"
      )
    }

    "handling an invalid directory" in {
      val directory = "utils/src/test/resources/fileUtils/animals.json"
      (the[IllegalArgumentException] thrownBy {
        FileUtils.getFiles(directory)
      } must have).message(s"requirement failed: '$directory' is not a directory")
    }
  }

}
