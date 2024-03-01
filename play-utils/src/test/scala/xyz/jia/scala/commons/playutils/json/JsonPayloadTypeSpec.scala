package xyz.jia.scala.commons.playutils.json

import scala.io.Source

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._

class JsonPayloadTypeSpec extends AnyWordSpec with BeforeAndAfterEach with MockitoSugar {

  import AccountType.AccountType

  trait Account {

    val `type`: AccountType

  }

  object Account {

    implicit def formats: Format[Account] = new Format[Account] {

      override def writes(account: Account): JsValue = account.`type`.toJson(account)

      override def reads(json: JsValue): JsResult[Account] =
        (json \ "type").validate[AccountType].flatMap(_.fromJson(json))

    }

  }

  object AccountType extends JsonPayloadType[Account] {

    type AccountType = JsonPayloadType

    val UserAccount: AccountType =
      JsonPayloadTypeVal(0, "UserAccount", toJson[UserAccount], fromJson[UserAccount])

    val CompanyAccount: AccountType =
      JsonPayloadTypeVal(1, "CompanyAccount", toJson[CompanyAccount], fromJson[CompanyAccount])

  }

  case class CompanyAccount(
      name: String,
      accountNumber: String,
      override val `type`: AccountType = AccountType.CompanyAccount
  ) extends Account

  object CompanyAccount {

    implicit def jsonFormats: OFormat[CompanyAccount] = Json.format[CompanyAccount]

  }

  case class UserAccount(
      product: String,
      productUserId: String,
      globalUserId: Option[String],
      override val `type`: AccountType = AccountType.UserAccount
  ) extends Account

  object UserAccount {

    implicit def jsonFormats: OFormat[UserAccount] = Json.format[UserAccount]

  }

  "jsonFormats" should {
    val companyAccount: Account = CompanyAccount("Jia", "001001234567")
    val companyAccountJson: JsValue =
      Json.parse(Source.fromResource("json/sampleCompanyAccount.json").getLines().mkString("\n"))
    val userAccount: Account = UserAccount("Supply", "123456", Some("987654"))
    val userAccountJson: JsValue =
      Json.parse(Source.fromResource("json/sampleUserAccount.json").getLines().mkString("\n"))

    "correctly serialize a subclass of the associated superclass" in {
      withClue("serialize example CompanyAccount") {
        Json.toJson(companyAccount)(Account.formats) mustEqual companyAccountJson
      }
      withClue("serialize example UserAccount") {
        Json.toJson(userAccount)(Account.formats) mustEqual userAccountJson
      }
    }

    "correctly deserialize a subclass of the associated superclass from JSON" in {
      withClue("deserialize example CompanyAccount") {
        companyAccountJson.as[Account] mustEqual companyAccount
      }
      withClue("deserialize example UserAccount") {
        userAccountJson.as[Account] mustEqual userAccount
      }
    }
  }

}
