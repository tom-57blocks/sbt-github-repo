package xyz.jia.scala.commons.playutils.tasks

import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.{Configuration, Environment}

class HttpInvocableTaskConfigValidationModuleSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach {

  val mockEnvironment: Environment = mock[Environment]

  "configure" should {
    "gracefully handle no http invocable task configuration being provided" in {
      val module = new HttpInvocableTaskConfigValidationModule(mockEnvironment, Configuration.empty)
      noException must be thrownBy module.configure()
    }

    "handle invalid http invocable task configuration being provided" in {
      val invalidConfig = ConfigFactory.parseString(
        """
          |httpInvocableTasks : {}
          |""".stripMargin
      )
      val configuration = new Configuration(invalidConfig)
      val module = new HttpInvocableTaskConfigValidationModule(mockEnvironment, configuration)
      val exception = intercept[IllegalArgumentException](module.configure())
      exception.getMessage.contains("Invalid or missing config requestsRepo.") mustBe true
    }

    "handle valid http invocable task configuration being provided" in {
      val configuration =
        new Configuration(ConfigFactory.parseResources("tasks/sampleValidTask.conf").resolve())
      val module = new HttpInvocableTaskConfigValidationModule(mockEnvironment, configuration)
      noException must be thrownBy module.configure()
    }
  }

}
