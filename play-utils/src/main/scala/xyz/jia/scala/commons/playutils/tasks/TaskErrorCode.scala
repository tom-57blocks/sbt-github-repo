package xyz.jia.scala.commons.playutils.tasks

object TaskErrorCode extends Enumeration {

  type TaskErrorCode = Value

  implicit def toVal(x: Value): TaskErrorCodeVal = x.asInstanceOf[TaskErrorCodeVal]

  protected case class TaskErrorCodeVal(
      override val id: Int,
      code: String,
      severity: Int
  ) extends super.Val(id, code) {

    override def compare(that: TaskErrorCode): Int = this.severity.compare(that.severity)

  }

  val ValidationFailed: TaskErrorCode = TaskErrorCodeVal(id = 0, code = "4000", severity = 4000)

  val DuplicateRequest: TaskErrorCode = TaskErrorCodeVal(id = 1, code = "4001", severity = 4001)

  val InternalError: TaskErrorCode = TaskErrorCodeVal(id = 2, code = "5000", severity = 5000)

}
