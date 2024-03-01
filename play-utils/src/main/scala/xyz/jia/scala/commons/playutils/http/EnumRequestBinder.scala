package xyz.jia.scala.commons.playutils.http

import play.api.Logging
import play.api.mvc.{PathBindable, QueryStringBindable}

trait EnumRequestBinder extends Logging {

  // $COVERAGE-OFF$ Low risk of failing given pathBindable has been tested.
  def queryStringBindable[E <: Enumeration](
      parse: String => E#Value,
      serialize: E#Value => String = (value: E#Value) => value.toString,
      handleError: (String, Exception) => String = errorHandler
  ): QueryStringBindable.Parsing[E#Value] =
    new QueryStringBindable.Parsing[E#Value](parse, serialize, handleError)

  // $COVERAGE-ON$

  def pathBindable[E <: Enumeration](
      parse: String => E#Value,
      serialize: E#Value => String = (value: E#Value) => value.toString,
      handleError: (String, Exception) => String = errorHandler
  ): PathBindable.Parsing[E#Value] =
    new PathBindable.Parsing[E#Value](parse, serialize, handleError)

  private def errorHandler(parameter: String, ex: Exception): String = {
    logger.debug(s"Unsupported value provided for parameter $parameter", ex)
    s"Unsupported value provided for parameter $parameter. ${ex.getMessage}"
  }

  /** Finds an `Enum` value whose name matches the provided name disregarding case sensitivity.
    *
    * Note that this method requires that the `.toString`` method of the `Enum` values returns their
    * names.
    * @param domain
    *   The business domain being represented by the enum. This is used when reporting errors. An
    *   example business domain is `SupportedCompany`.
    * @param enumeration
    *   The enum whose values to search from
    * @param name
    *   The value to use for looking up the `Enum` value
    * @tparam E
    * @return
    *   The found `Enum` value. Throws an Illegal argument exception if no value is found matching
    *   the provided name
    */
  def findByNameIgnoreCase[E <: Enumeration](
      domain: String,
      enumeration: E,
      name: String
  ): E#Value = {
    enumeration.values
      .find(_.toString.toLowerCase == name.toLowerCase())
      .getOrElse(throw new IllegalArgumentException(s"Unsupported $domain $name"))
  }

}

object EnumRequestBinder extends EnumRequestBinder
