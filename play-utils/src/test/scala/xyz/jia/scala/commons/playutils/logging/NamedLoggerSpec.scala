package xyz.jia.scala.commons.playutils.logging

import scala.io.Source

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeRequest

import xyz.jia.scala.commons.playutils.json.JsonMaskConfig

class NamedLoggerSpec extends AnyWordSpec with Matchers {

  "httpRequestAppender" should {
    "correctly log request details" in {
      val request = FakeRequest(
        method = "GET",
        uri = "/user/search?telephone=1339&date=2022-02-17",
        headers = Headers(("X-Request-Id", "abce"), ("X-Password", "xyz")),
        body = Json.obj("telephone" -> "1235", "name" -> "lex", "idNumber" -> "9872")
      )

      implicit val maskConfig: JsonMaskConfig =
        new JsonMaskConfig(sensitiveInfoPaths = List("password", "telephone"))

      val expectedPayload =
        Json.parse(Source.fromResource("logging/maskedRequest.json").getLines().mkString("\n"))

      NamedLogger.httpRequestAppender(request).toString mustEqual s"request=$expectedPayload"
    }
  }

  "correlationIdAppender" should {
    "correctly log a supplied correlation id" in {
      NamedLogger.correlationIdAppender("chicken").toString mustEqual s"correlationId=chicken"
    }
  }

  "rawJsonAppender" should {
    "mask and log a json payload" in {
      implicit val maskConfig: JsonMaskConfig =
        new JsonMaskConfig(sensitiveInfoPaths = List("password", "telephone"))

      val json = Json.obj("telephone" -> "1235", "name" -> "lex")
      val expectedPayload = Json.obj("telephone" -> "************", "name" -> "lex")
      NamedLogger.rawJsonAppender("abc-cat", json).toString mustEqual s"abc-cat=$expectedPayload"
    }
  }

  "configAppender" should {
    "correctly log config details" in {
      val unmaskedConfig = ConfigFactory.parseResources("logging/unmaskedConfig.conf")
      implicit val maskConfig: JsonMaskConfig =
        new JsonMaskConfig(List("phoneNumber", "X-Request-ID"))

      val maskedConfig = ConfigFactory.parseResources("logging/maskedConfig.conf")
      val expectedJson = Json.parse(maskedConfig.root().render(ConfigRenderOptions.concise()))
      println(NamedLogger.configAppender(unmaskedConfig).toString)
      NamedLogger.configAppender(unmaskedConfig).toString mustEqual s"config=$expectedJson"
    }
  }

}
