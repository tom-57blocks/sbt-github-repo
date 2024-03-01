package xyz.jia.scala.commons.playutils.json

import play.api.libs.json._

/** Provides the ability to JSON serialize/deserialize a superclass from/to its subclasses without
  * the need to pattern match against all its subclasses. Below is a sample usage
  * {{{
  *
  *   trait Account {
  *     val `type`: AccountType
  *   }
  *
  *   object Account {
  *     implicit def formats: Format[Account] = new Format[Account] {
  *       override def writes(account: Account): JsValue = account.`type`.toJson(account)
  *       override def reads(json: JsValue): JsResult[Account] =
  *         (json \ "type").validate[AccountType].flatMap(_.fromJson(json))
  *     }
  *   }
  *
  *   object AccountType extends JsonPayloadType[Account] {
  *     type AccountType = JsonPayloadType
  *     val UserAccount: AccountType =
  *       JsonPayloadTypeVal(0, "UserAccount", toJson[UserAccount], fromJson[UserAccount])
  *     val CompanyAccount: AccountType =
  *       JsonPayloadTypeVal(1, "CompanyAccount", toJson[CompanyAccount], fromJson[CompanyAccount])
  *   }
  *
  *   case class CompanyAccount(
  *     name: String,
  *     accountNumber: String,
  *     override val `type`: AccountType = AccountType.CompanyAccount
  *   ) extends Account
  *
  *   object CompanyAccount {
  *     implicit def jsonFormats: OFormat[CompanyAccount] = Json.format[CompanyAccount]
  *   }
  *
  *   case class UserAccount(
  *     product: String,
  *     productUserId: String,
  *     globalUserId: Option[String],
  *     override val `type`: AccountType = AccountType.UserAccount
  *   ) extends Account
  *
  *   object UserAccount {
  *     implicit def jsonFormats: OFormat[UserAccount] = Json.format[UserAccount]
  *   }
  *
  * }}}
  *
  * @tparam Payload
  *   The abstract payload that will be processed against the type
  */
class JsonPayloadType[Payload] extends Enumeration {

  type JsonPayloadType = Value

  /** @param id
    *   The unique ID of the payload type in the enum list
    * @param name
    *   The unique name used to identify the payload type in the enum list. This is used to
    *   determine the deserializer being targeted when parsing `JSON` content.
    * @param toJson
    *   the method to use when converting the subclass of `Payload` represented by this type
    * @param fromJson
    *   the method to use when converting `JSON` content to `Payload` parent class
    */
  protected case class JsonPayloadTypeVal(
      override val id: Int,
      name: String,
      toJson: Payload => JsValue,
      fromJson: JsValue => JsResult[Payload]
  ) extends super.Val {

    override def toString(): String = name

  }

  implicit def toVal(x: Value): JsonPayloadTypeVal = x.asInstanceOf[JsonPayloadTypeVal]

  /** Convenience method for creating a `JSON` representation of super class `Payload` from its
    * subclass `T`
    */
  def toJson[T <: Payload](payload: Payload)(implicit writes: Writes[T]): JsValue =
    writes.writes(payload.asInstanceOf[T])

  /** Convenience method for creating superclass `Payload` from a `JSON` representation of its
    * subclass `T`
    */
  def fromJson[T <: Payload](json: JsValue)(implicit reads: Reads[T]): JsResult[Payload] =
    reads.reads(json).map(_.asInstanceOf[Payload])

  implicit def jsonFormats: Format[JsonPayloadType] = Json.formatEnum(this)

}
