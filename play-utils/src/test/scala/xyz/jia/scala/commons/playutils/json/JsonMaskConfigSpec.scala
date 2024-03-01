package xyz.jia.scala.commons.playutils.json

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class JsonMaskConfigSpec extends AnyWordSpec with Matchers {

  "configLoader" should {
    "correctly parse valid configuration" in {
      val config = ConfigFactory.parseResources("json/masking/sampleMaskConfig.conf")
      new Configuration(config).get[JsonMaskConfig]("logMasking") mustEqual
        JsonMaskConfig(List("a", "x", "m"))
    }
  }

}
