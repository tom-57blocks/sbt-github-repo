package xyz.jia.scala.commons.play

import scala.concurrent.ExecutionContext

/** Execution context used in testing of functionality that requires an execution context
  * implementation to be provided.
  */
class DummyTestExecutionContext extends ExecutionContext {

  override def execute(runnable: Runnable): Unit =
    ExecutionContext.Implicits.global.execute(runnable)

  override def reportFailure(cause: Throwable): Unit =
    ExecutionContext.Implicits.global.reportFailure(cause)

}
