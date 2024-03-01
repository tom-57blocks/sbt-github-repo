package xyz.jia.scala.commons.playutils

import play.api.libs.json.JsError
import xyz.jia.scala.commons.utils.ErrorMessage

object ErrorMessageHelpers {

  implicit class JsErrorAsErrorMessage(error: JsError) {

    def errorMessages: Seq[ErrorMessage] = {
      JsError.toFlatForm(error).toSeq.map { case (path, validationErrors) =>
        ErrorMessage(s"$path => ${validationErrors.map(_.message).mkString(",")}")
      }
    }

  }

}
