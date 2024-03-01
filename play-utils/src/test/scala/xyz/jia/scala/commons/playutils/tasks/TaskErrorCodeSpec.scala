package xyz.jia.scala.commons.playutils.tasks

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import xyz.jia.scala.commons.playutils.tasks.TaskErrorCode._

class TaskErrorCodeSpec extends AnyWordSpec with Matchers {

  "taskErrorCode" should {
    "correctly get value for a valid task error code" in {
      TaskErrorCode.values.foreach { taskErrorCode: TaskErrorCode =>
        TaskErrorCode.toVal(taskErrorCode) mustEqual taskErrorCode
      }
    }
  }

  "compare" should {
    "correctly compare two task error codes based on their severity" in {
      List(ValidationFailed, DuplicateRequest).max mustEqual DuplicateRequest
      List(InternalError, DuplicateRequest).max mustEqual InternalError
      List(ValidationFailed, InternalError).max mustEqual InternalError
    }
  }

}
