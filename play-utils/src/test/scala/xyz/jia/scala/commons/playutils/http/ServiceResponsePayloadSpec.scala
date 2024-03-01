package xyz.jia.scala.commons.playutils.http

import scala.io.Source

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsArray, JsString, Json}

import xyz.jia.scala.commons.playutils.http.ServiceResponsePayload.Metadata

class ServiceResponsePayloadSpec extends AnyWordSpec {

  "payloadFormats" should {
    val responsePayload = ServiceResponsePayload(
      data = Json.obj("id" -> 160),
      meta = Metadata(
        errors = Some(JsArray(Seq(JsString("name.required"), JsString("age.invalid")))),
        executionTime = 1834,
        requestId = "411d6fe6-5065-463a-9708-531e80dc4379",
        resultCode = "2000"
      )
    )
    val payloadResource = Source.fromResource("http/sampleServiceResponsePayload.json")
    val responsePayloadJson = Json.parse(payloadResource.getLines().mkString("\n"))

    "correctly serialize a ServiceResponsePayload to JSON" in {
      Json.toJson(responsePayload) mustEqual responsePayloadJson
    }

    "correctly deserialize a ServiceResponsePayload from JSON" in {
      responsePayloadJson.as[ServiceResponsePayload] mustEqual responsePayload
    }
  }

}
