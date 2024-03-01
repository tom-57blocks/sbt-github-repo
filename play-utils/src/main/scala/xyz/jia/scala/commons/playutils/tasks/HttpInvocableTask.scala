package xyz.jia.scala.commons.playutils.tasks

import scala.concurrent.Future

import akka.Done
import play.api.libs.json.JsValue

import xyz.jia.scala.commons.utils.ErrorMessage

/** An implementation of an HTTP invocable task */
trait HttpInvocableTask {

  type DataType

  def parseData(data: JsValue): Future[Either[Seq[ErrorMessage], DataType]]

  def process(data: DataType): Future[Done]

}
