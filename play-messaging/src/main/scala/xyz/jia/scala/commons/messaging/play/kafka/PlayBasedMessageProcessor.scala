package xyz.jia.scala.commons.messaging.play.kafka

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

import play.api.cache.AsyncCacheApi

import xyz.jia.scala.commons.messaging.kafka.MessageProcessor

trait PlayBasedMessageProcessor[K, V] extends MessageProcessor[K, V] {

  val asyncCacheApi: AsyncCacheApi

  implicit val ec: ExecutionContext

  override def getOrInitProcessingAttempts(
      messageChecksum: String,
      cacheTtl: Duration,
      defaultAttempts: Int
  ): Future[Int] = {
    asyncCacheApi.get[Int](messageChecksum).flatMap {
      case Some(value) => Future.successful(value)
      case None =>
        asyncCacheApi.set(messageChecksum, defaultAttempts, cacheTtl).map(_ => defaultAttempts)
    }
  }

  override def incrementProcessingAttempts(
      messageChecksum: String,
      cacheTtl: Duration
  ): Future[Int] = {
    getOrInitProcessingAttempts(messageChecksum, cacheTtl, 1).flatMap { attempts =>
      val incrementedAttempts = attempts + 1
      asyncCacheApi
        .set(messageChecksum, incrementedAttempts, cacheTtl)
        .map(_ => incrementedAttempts)
    }
  }

}
