package xyz.jia.scala.commons.playutils.tasks

import scala.jdk.CollectionConverters.ListHasAsScala

import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import xyz.jia.scala.commons.test.NthAnswer
import xyz.jia.scala.commons.utils.ErrorMessage

class HttpInvocableTaskConfigParserSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with NthAnswer
    with MockitoSugar {

  class ConfigParserStub extends HttpInvocableTaskConfigParser

  val invalidConfigList: Config = ConfigFactory.parseResources("tasks/sampleInvalidTasks.conf")

  "validateBaseConfig" should {
    "accept a valid HTTP invocable task configuration" in {
      val validConfig: Config =
        ConfigFactory.parseResources("tasks/sampleValidTask.conf").getConfig("httpInvocableTasks")
      new ConfigParserStub().validateBaseConfig(validConfig) mustBe Seq.empty
    }

    "reject task configuration with missing tasks definition" in {
      val errorMessages =
        new ConfigParserStub().validateBaseConfig(invalidConfigList.getConfig("missingTasks"))
      errorMessages.size mustEqual 1
      errorMessages.head.message.contains(
        "No configuration setting found for key 'tasks'"
      ) mustBe true
    }

    "reject task configuration with missing requestsRepo definition" in {
      val errorMessages = new ConfigParserStub().validateBaseConfig(
        invalidConfigList.getConfig("missingRequestsRepo")
      )
      errorMessages.size mustEqual 1
      errorMessages.head.message.contains(
        "No configuration setting found for key 'requestsRepo'"
      ) mustBe true
    }

    "reject task configuration with invalid tasks definitions" in {
      val spyParser = spy(new ConfigParserStub())
      doAnswer(
        nthAnswer(
          Left(Seq(ErrorMessage("1"))),
          Right(mock[HttpInvocableTaskConfig]),
          Left(Seq(ErrorMessage("2")))
        )
      ).when(spyParser).parseTaskConfig(any[Config]())
      val errorMessages = spyParser.validateBaseConfig(invalidConfigList.getConfig("invalidTasks"))
      errorMessages.size mustEqual 2
      errorMessages must contain allElementsOf Seq("1", "2").map(ErrorMessage)
      verify(spyParser, times(3)).parseTaskConfig(any[Config]())
    }
  }

  "parseRequestsRepo" when {
    "given a valid configuration" should {
      "correctly process a config" in {
        val configuration = new Configuration(
          ConfigFactory
            .parseResources("tasks/sampleValidTask.conf")
            .resolve()
        )

        val implementation = "xyz.jia.scala.commons.playutils.tasks.DummyTaskStorageService"
        new ConfigParserStub().parseRequestsRepo(configuration) mustBe Right(implementation)
      }
    }

    "given invalid configuration" should {
      "reject an empty configuration" in {
        new ConfigParserStub().parseRequestsRepo(Configuration.empty) mustBe
          Left(Seq(ErrorMessage("No HTTP Invocable Task Configuration provided")))
      }

      "reject a configuration missing RequestRepo key" in {
        val spyParser = spy(new ConfigParserStub())
        val errorMessages = Seq("Oops", "Aah").map(ErrorMessage)
        doReturn(errorMessages).when(spyParser).validateBaseConfig(any[Config]())

        val configuration =
          new Configuration(ConfigFactory.parseResources("tasks/sampleValidTask.conf").resolve())
        spyParser.parseRequestsRepo(configuration) mustBe Left(errorMessages)
      }
    }
  }

  "parseConfiguration" should {
    "gracefully handle no task configuration being provided" in {
      new ConfigParserStub().parseConfiguration(Configuration.empty) mustBe Right(Nil)
    }

    "report errors in parsing the entire configuration" in {
      val spyParser = spy(new ConfigParserStub())
      val errorMessages = Seq("Ape", "Food").map(ErrorMessage)
      doReturn(errorMessages).when(spyParser).validateBaseConfig(any[Config]())
      val config = ConfigFactory.parseResources("tasks/sampleValidTask.conf")
      spyParser.parseConfiguration(new Configuration(config)) mustBe Left(errorMessages)
      verify(spyParser).validateBaseConfig(config.getConfig("httpInvocableTasks"))
      verify(spyParser, never()).parseTaskConfig(any[Config]())
    }

    "report errors in parsing individual task configs" in {
      val spyParser = spy(new ConfigParserStub())
      doReturn(Nil).when(spyParser).validateBaseConfig(any[Config]())
      val config = ConfigFactory.parseResources("tasks/sampleValidTask.conf")
      val errorMessages = Seq("Oops", "Banana").map(ErrorMessage)
      doAnswer(
        nthAnswer(
          Right(mock[HttpInvocableTaskConfig]),
          Left(Seq(errorMessages.head)),
          Left(errorMessages.tail)
        )
      ).when(spyParser).parseTaskConfig(any[Config]())

      spyParser.parseConfiguration(new Configuration(config)) mustBe Left(errorMessages)
      verify(spyParser, times(3)).parseTaskConfig(any[Config]())
      config.getConfigList("httpInvocableTasks.tasks").asScala.foreach { taskConfig =>
        verify(spyParser).parseTaskConfig(taskConfig)
      }
    }

    "handle valid configuration" in {
      val spyParser = spy(new ConfigParserStub())
      val taskConfig = Seq(
        mock[HttpInvocableTaskConfig],
        mock[HttpInvocableTaskConfig],
        mock[HttpInvocableTaskConfig]
      )

      doReturn(Nil).when(spyParser).validateBaseConfig(any[Config]())
      doAnswer(nthAnswer(taskConfig.map(Right(_)): _*))
        .when(spyParser)
        .parseTaskConfig(any[Config]())
      val config = ConfigFactory.parseResources("tasks/sampleValidTask.conf")
      spyParser.parseConfiguration(new Configuration(config)) mustBe Right(taskConfig)
    }
  }

  "parseTaskConfig" should {
    "reject configuration without required keys" in {
      val result = new ConfigParserStub().parseTaskConfig(ConfigFactory.empty())
      result.isLeft mustBe true
      val errorMessages = result.swap.toOption.get
      errorMessages.exists(
        _.message.contains("No configuration setting found for key 'task'")
      ) mustBe true
      errorMessages.exists(
        _.message.contains("No configuration setting found for key 'processor'")
      ) mustBe true
    }

    "reject configuration with processor class that doesn't exist" in {
      val config = ConfigFactory.parseString(
        """
          |{
          |  "task": "abc"
          |  "processor": "Kitten"
          |}
          |""".stripMargin
      )

      val errors = Seq(ErrorMessage("ClassNotFoundException : Kitten"))
      new ConfigParserStub().parseTaskConfig(config) mustBe Left(errors)
    }

    "reject configuration with task key of invalid type" in {
      val config = ConfigFactory.parseString(
        s"""
           |{
           |  "task": { "name": "demoTask" } 
           |  "processor": "xyz.jia.scala.commons.playutils.tasks.DummyTask"
           |}
           |""".stripMargin
      )

      val msg =
        "Invalid or missing config task. Error details => String: 3: task has type OBJECT rather than STRING"
      new ConfigParserStub().parseTaskConfig(config) mustBe Left(Seq(ErrorMessage(msg)))
    }

    "correctly parse valid configuration" in {
      val validConfig = ConfigFactory.parseString(
        """
          |{
          |  "task": "abc"
          |  "processor": "xyz.jia.scala.commons.playutils.tasks.DummyTask"
          |}
          |""".stripMargin
      )

      new ConfigParserStub().parseTaskConfig(validConfig) mustBe Right(
        HttpInvocableTaskConfig(
          taskName = "abc",
          implementation = "xyz.jia.scala.commons.playutils.tasks.DummyTask"
        )
      )
    }

  }

}
