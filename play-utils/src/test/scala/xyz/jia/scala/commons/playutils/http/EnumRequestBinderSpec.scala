package xyz.jia.scala.commons.playutils.http

import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.HttpVerbs
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc.{PathBindable, QueryStringBindable}
import play.api.routing.Router
import play.api.routing.sird.{GET, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class EnumRequestBinderSpec extends AnyWordSpec {

  import SupportedCompany.SupportedCompany

  "pathBindable" should {
    val application = new GuiceApplicationBuilder().router {
      val companyExtractor = new PathBindableExtractor[SupportedCompany]
      Router.from { case GET(p"fetch-company/${companyExtractor(company)}") =>
        stubControllerComponents().actionBuilder(Ok(s"Found company ${company.name.toUpperCase}"))
      }
    }.build()

    "correctly bind valid path variables" in {
      List("suPply", "FulfillmenT").foreach { testCompany =>
        val request = FakeRequest(HttpVerbs.GET, s"fetch-company/$testCompany")
        val home = route(application, request).get
        status(home) mustBe OK
        contentType(home) mustBe Some("text/plain")
        contentAsString(home) must include(s"Found company ${testCompany.toUpperCase}")
      }
    }

    "reject an invalid path parameter being attempted to be bound to an enum" in {
      val request = FakeRequest(HttpVerbs.GET, "fetch-company/kitten")
      val home = route(application, request).get

      /** in actual runs a 400 response is returned however in this case a 404 is enough to prove
        * the binder works as expected
        */
      status(home) mustBe NOT_FOUND
    }

  }

  object SupportedCompany extends Enumeration {

    type SupportedCompany = Value

    protected case class SupportedCompanyVal(
        override val id: Int,
        name: String
    ) extends super.Val {

      override def toString(): String = name

    }

    implicit def toVal(x: Value): SupportedCompanyVal = x.asInstanceOf[SupportedCompanyVal]

    val Supply: SupportedCompany = SupportedCompanyVal(0, "supply")

    val Fulfillment: SupportedCompany = SupportedCompanyVal(1, "Fulfillment")

    implicit def pathBindable: PathBindable[SupportedCompany] =
      EnumRequestBinder.pathBindable(EnumRequestBinder.findByNameIgnoreCase("company", this, _))

    implicit def queryBindable: QueryStringBindable[SupportedCompany] =
      EnumRequestBinder.queryStringBindable(
        EnumRequestBinder.findByNameIgnoreCase("company", this, _)
      )

  }

}
