package xyz.jia.scala.commons.playutils.http

import javax.inject.Inject

import scala.concurrent.Future

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import xyz.jia.scala.commons.playutils.http.ControllerUtils.Attrs

/** Adds request attributes convenient for services */
class CommonAttributesInclusionFilter @Inject() (
)(implicit override val mat: Materializer)
    extends Filter
    with ControllerUtils {

  def apply(
      nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = nextFilter {
    requestHeader
      .addAttr(Attrs.processingStartTime, systemNanoTime)
      .addAttr(Attrs.requestId, getRequestId(requestHeader))
  }

}
