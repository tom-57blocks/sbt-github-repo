package xyz.jia.scala.commons.playutils.json

import java.io.File
import java.nio.file.Path

import scala.util.{Failure, Success}

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._

import xyz.jia.scala.commons.playutils.FileUtils
import xyz.jia.scala.commons.test.NthAnswer
import xyz.jia.scala.commons.utils.ErrorMessage

class JsonConfigReaderSpec
    extends AnyWordSpec
    with BeforeAndAfterEach
    with MockitoSugar
    with NthAnswer {

  val configDirectory = "test-directory"

  case class SampleConfig(name: String, age: Int)

  class SampleConfigReader(override protected val fileReader: FileUtils)
      extends JsonConfigReader[SampleConfig] {

    implicit override protected def configJsonReads: Reads[SampleConfig] = sampleConfigFormats

  }

  implicit def sampleConfigFormats: OFormat[SampleConfig] = Json.format[SampleConfig]

  var mockFileUtils: FileUtils = _

  var configReader: JsonConfigReader[SampleConfig] = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockFileUtils = mock[FileUtils]

    configReader = new SampleConfigReader(mockFileUtils)
  }

  "loadConfigurations" should {

    "correctly accumulate errors and configurations" in {
      val configuration = SampleConfig("ivan", 167)

      val mockPath: Path = mock[Path]
      when(mockFileUtils.getFiles(any[String](), any[Int]())).thenReturn(List.fill(2)(mockPath))

      val spyReader = spy(configReader)
      val leftResult = Left(Seq(ErrorMessage("failed")))
      val rightResult = Right(configuration)
      doAnswer(nthAnswer(leftResult, rightResult))
        .when(spyReader)
        .readConfigurationFile(any[Path]())

      spyReader.loadConfigurations(configDirectory) mustBe
        (Seq(ErrorMessage("failed")), Seq(configuration))

      verify(mockFileUtils).getFiles(configDirectory, 2)
      verify(spyReader, times(2)).readConfigurationFile(mockPath)
    }
  }

  "readConfigurationFile" should {

    "correctly handle file read failure" in {
      when(mockFileUtils.getFileAsJson(any[File]()))
        .thenReturn(Failure(new Throwable("file not found")))

      val mockPath: Path = mock[Path]
      val mockFile: File = mock[File]
      when(mockPath.toFile).thenReturn(mockFile)
      when(mockPath.toString).thenReturn("test-path")

      val message = "Invalid JSON detected in configuration file: test-path. " +
        s"Error => file not found"

      configReader.readConfigurationFile(mockPath) mustBe Left(Seq(ErrorMessage(message)))

      verify(mockFileUtils).getFileAsJson(mockFile)
    }

    "correctly handle JSON validation error" in {
      val mockJsValue: JsValue = mock[JsValue]
      when(mockJsValue.validate[SampleConfig](any[Reads[SampleConfig]]()))
        .thenReturn(JsError("invalid file"))
      when(mockFileUtils.getFileAsJson(any[File]())).thenReturn(Success(mockJsValue))

      val mockPath: Path = mock[Path]
      val mockFile: File = mock[File]
      when(mockPath.toFile).thenReturn(mockFile)

      val message = "obj => invalid file"

      configReader.readConfigurationFile(mockPath) mustBe Left(Seq(ErrorMessage(message)))

      verify(mockFileUtils).getFileAsJson(mockFile)
    }

    "return the content of a valid file" in {
      val configuration = SampleConfig("ivan", 167)

      val mockJsValue: JsValue = mock[JsValue]
      when(mockJsValue.validate[SampleConfig](any[Reads[SampleConfig]]()))
        .thenReturn(JsSuccess(configuration))
      when(mockFileUtils.getFileAsJson(any[File]())).thenReturn(Success(mockJsValue))

      val mockPath: Path = mock[Path]
      val mockFile: File = mock[File]
      when(mockPath.toFile).thenReturn(mockFile)

      configReader.readConfigurationFile(mockPath) mustBe Right(configuration)

      verify(mockFileUtils).getFileAsJson(mockFile)
    }
  }

}
