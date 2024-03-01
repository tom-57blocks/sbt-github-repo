package xyz.jia.scala.commons.playutils.http.rest

import scala.concurrent.duration.DurationInt

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class RestCircuitConfigSpec extends AnyWordSpec with Matchers {

  "configLoader" should {
    "correctly parse valid configuration" in {
      val config = ConfigFactory.parseResources("http/rest/sampleRestCircuitConfig.conf")
      new Configuration(config).get[RestCircuitConfig]("rest-circuit-config") mustEqual
        RestCircuitConfig(5, 10.seconds, 1.minute, Set(200))
    }
  }

}
