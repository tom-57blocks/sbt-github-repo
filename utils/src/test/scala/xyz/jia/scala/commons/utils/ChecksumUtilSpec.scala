package xyz.jia.scala.commons.utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ChecksumUtilSpec extends AnyWordSpec with Matchers {

  val dataAndChecksum: (String, String) =
    ("sample-data", "c45df724cba87ac84892bf3eeb910393d69d163e9ad96cac2e2074487eaa907b")

  val data2AndChecksum: (String, String) =
    ("sample-data-2", "acbd0f0b2c54b326a721430ffd6e0d94037d1188449dc9895a9980d1e9449062")

  "checksum" should {

    "correctly generate checksum when string data is provided" in {
      ChecksumUtil.checksum(dataAndChecksum._1) mustEqual dataAndChecksum._2
      ChecksumUtil.checksum(data2AndChecksum._1) mustEqual data2AndChecksum._2

      withClue("checksum should be unique for dissimilar data") {
        ChecksumUtil
          .checksum(dataAndChecksum._1)
          .equals(ChecksumUtil.checksum(data2AndChecksum._1)) mustBe false
      }
    }

    "correctly generate checksum when byte array data is provided" in {
      ChecksumUtil.checksum(dataAndChecksum._1.getBytes) mustEqual dataAndChecksum._2
      ChecksumUtil.checksum(data2AndChecksum._1.getBytes) mustEqual data2AndChecksum._2

      withClue("checksum should be unique for dissimilar data") {
        ChecksumUtil
          .checksum(dataAndChecksum._1.getBytes)
          .equals(ChecksumUtil.checksum(data2AndChecksum._1.getBytes)) mustBe false
      }
    }
  }

}
