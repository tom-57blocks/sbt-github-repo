package xyz.jia.scala.commons.messaging.play.avro

import com.sksamuel.avro4s.{Codec, SchemaFor}
import org.apache.avro.SchemaBuilder
import play.api.libs.json.{JsValue, Json}

object JsValueCodec {

  val jsValueSchemaFor: SchemaFor[JsValue] =
    SchemaFor[JsValue](SchemaBuilder.builder().stringType())

  val jsValueCodec: Codec[JsValue] = new Codec[JsValue] {

    override def decode(value: Any): JsValue = Json.parse(value.toString)

    override def encode(value: JsValue): String = value.toString()

    override def schemaFor: SchemaFor[JsValue] = jsValueSchemaFor

  }

}
