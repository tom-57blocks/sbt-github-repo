package xyz.jia.scala.commons.playutils

import java.time.LocalDateTime

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, Json, Reads}

import xyz.jia.scala.commons.playutils.ErrorMessageHelpers.JsErrorAsErrorMessage
import xyz.jia.scala.commons.utils.ErrorMessage

class ErrorMessageHelpersSpec extends AnyWordSpec with Matchers {

  case class DummyFooModel(foo: Int, bar: String, baz: LocalDateTime)

  implicit val dummyModelReads: Reads[DummyFooModel] = Json.reads[DummyFooModel]

  "JsErrorAsErrorMessage#errorMessages" should {
    "correctly convert a JsError to ErrorMessages" in {
      val json = """{"baz":"123"}"""
      val sampleJsError = Json.parse(json).validate[DummyFooModel].asEither.swap.getOrElse(Nil)
      JsError(sampleJsError).errorMessages mustEqual Seq(
        ErrorMessage("obj.foo => error.path.missing"),
        ErrorMessage("obj.bar => error.path.missing"),
        ErrorMessage("obj.baz => error.expected.date.isoformat")
      )
    }
  }

}
