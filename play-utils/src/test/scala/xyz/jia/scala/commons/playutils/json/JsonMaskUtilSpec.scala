package xyz.jia.scala.commons.playutils.json

import scala.io.Source

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, JsValue, Json}

class JsonMaskUtilSpec extends AnyWordSpec with Matchers {

  def readResourceAsJson(path: String): JsValue =
    Json.parse(Source.fromResource(path).getLines().mkString("\n"))

  "maskSensitiveValues" should {
    "correctly mask all specified sensitive info" in {
      withClue("selective mask") {
        JsonMaskUtil.maskSensitiveValues(
          unmaskedJson = readResourceAsJson("json/masking/sampleUnmaskedPayload.json"),
          sensitiveInfoPaths = List(
            "phoneNumber", // test replacing multiple entries at various locations
            "/metadata/homeAddress", // test replacing absolute nested path
            "homeAddress", // verify order of mask enforcement
            "metadata/alternativePhones", // test replacing at absolute path using a relative link
            "X-Request-ID", "/kittens" // test replacing non existent value
          )
        ) mustEqual readResourceAsJson("json/masking/sampleMaskedPayload.json")
      }

      withClue("blanket mask") {
        JsonMaskUtil.maskSensitiveValues(
          unmaskedJson = readResourceAsJson("json/masking/sampleUnmaskedPayload.json"),
          sensitiveInfoPaths = List("*")
        ) mustEqual JsString("************")
      }

      withClue("empty list") {
        JsonMaskUtil.maskSensitiveValues(
          unmaskedJson = readResourceAsJson("json/masking/sampleUnmaskedPayload.json"),
          sensitiveInfoPaths = List.empty
        ) mustEqual readResourceAsJson("json/masking/sampleUnmaskedPayload.json")

        JsonMaskUtil.maskSensitiveValues(
          unmaskedJson = readResourceAsJson("json/masking/sampleUnmaskedPayload.json"),
          sensitiveInfoPaths = List("")
        ) mustEqual readResourceAsJson("json/masking/sampleUnmaskedPayload.json")
      }
    }
  }

}
