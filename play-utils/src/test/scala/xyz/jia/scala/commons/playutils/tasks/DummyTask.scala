package xyz.jia.scala.commons.playutils.tasks

import java.time.OffsetDateTime

import scala.concurrent.Future

import akka.Done
import play.api.libs.json.JsValue

import xyz.jia.scala.commons.utils.ErrorMessage

class DummyTask extends HttpInvocableTask {

  override type DataType = OffsetDateTime

  override def parseData(data: JsValue): Future[Either[Seq[ErrorMessage], OffsetDateTime]] = ???

  override def process(data: OffsetDateTime): Future[Done] = ???

}
