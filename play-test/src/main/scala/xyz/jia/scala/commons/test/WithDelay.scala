package xyz.jia.scala.commons.test

import scala.concurrent.duration.FiniteDuration

import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit}

abstract class WithDelay extends ScalaTestWithActorTestKit(ManualTime.config) {

  val manualTime: ManualTime = ManualTime()

  def withDelay[T](duration: FiniteDuration)(fun: => T): T = {
    manualTime.timePasses(duration)
    fun
  }

}
