package xyz.jia.scala.commons.playutils.http.rest

import java.util.Base64

import scala.concurrent.ExecutionContext.Implicits.global

import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api
import play.api.http.Writeable
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.mvc.{Handler, Request, RequestHeader}
import play.api.test.Helpers.AUTHORIZATION
import play.api.test._
import play.api.{BuiltInComponents, Configuration}
import play.core.server.ServerProvider.fromConfiguration
import play.core.server.{Server, ServerProvider}

import xyz.jia.scala.commons.playutils.json.JsonMaskConfig

class RestClientSpec
    extends AnyWordSpec
    with WsTestClient
    with ScalaFutures
    with Matchers
    with GuiceOneAppPerTest {

  implicit val patience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  implicit val maskConfig: JsonMaskConfig = JsonMaskConfig(List("body", "headers"))

  implicit def mat: Materializer = app.materializer

  "get" when {
    "only given a url" should {
      "correctly make a GET request" in {
        val url = "/test"
        val expectedResponse = Json.obj("key" -> "value")
        withRestService(expectedResponse)(verifyRequest(_, "GET", url)) { client =>
          whenReady(client.get(url)) { response =>
            response.json mustEqual expectedResponse
          }
        }
      }
    }

    "given headers" should {
      "correctly make a GET request" in {
        val url = "/test"
        val headers = Map("key" -> "value")
        val expectedResponse = Json.obj("key" -> "value")
        withRestService(expectedResponse)(verifyRequest(_, "GET", url, headers = headers)) {
          client =>
            whenReady(client.get(url, headers = headers)) { response =>
              response.json mustEqual expectedResponse
            }
        }
      }
    }

    "given query parameters" should {
      "correctly make a GET request" in {
        val url = "/test"
        val expectedUrl = "/test?key=value"
        val queryParams = Map("key" -> "value")
        val expectedResponse = Json.obj("key" -> "value")
        withRestService(expectedResponse) {
          verifyRequest(_, "GET", expectedUrl, queryParams = queryParams)
        } { client =>
          whenReady(client.get(url, queryParams = queryParams)) { response =>
            response.json mustEqual expectedResponse
          }
        }
      }
    }

    "given auth configurations" should {
      "correctly make a GET request" in {
        val url = "/test"
        val authConfig = AuthConfig("user", "pass", BASIC)
        val expectedResponse = Json.obj("key" -> "value")
        withRestService(expectedResponse) {
          verifyRequest(_, "GET", url, auth = Some(authConfig))
        } { client =>
          whenReady(client.get(url, auth = Some(authConfig))) { response =>
            response.json mustEqual expectedResponse
          }
        }
      }
    }
  }

  "post" when {
    "only given a url and body" should {
      "correctly make a POST request" in {
        val url = "/test"
        val body = Json.obj("key" -> "value")
        val expectedResponse = Json.obj("key" -> "value")
        withRestService(expectedResponse)(verifyRequest(_, "POST", url, body = Some(body))) {
          client =>
            whenReady(client.post(url, body)) { response =>
              response.json mustEqual expectedResponse
            }
        }
      }
    }

    "given headers" should {
      "correctly make a POST request" in {
        val url = "/test"
        val body = Json.obj("key" -> "value")
        val headers = Map("key" -> "value")
        val expectedResponse = Json.obj("key" -> "value")
        withRestService(expectedResponse) {
          verifyRequest(_, "POST", url, headers = headers, body = Some(body))
        } { client =>
          whenReady(client.post(url, body, headers = headers)) { response =>
            response.json mustEqual expectedResponse
          }
        }
      }
    }

    "given query parameters" should {
      "correctly make a POST request" in {
        val url = "/test"
        val expectedUrl = "/test?key=value"
        val body = Json.obj("key" -> "value")
        val queryParams = Map("key" -> "value")
        val expectedResponse = Json.obj("key" -> "value")
        withRestService(expectedResponse) {
          verifyRequest(_, "POST", expectedUrl, queryParams = queryParams, body = Some(body))
        } { client =>
          whenReady(client.post(url, body, queryParams = queryParams)) { response =>
            response.json mustEqual expectedResponse
          }
        }
      }
    }

    "given auth configurations" should {
      "correctly make a POST request" in {
        val url = "/test"
        val body = Json.obj("key" -> "value")
        val authConfig = AuthConfig("user", "pass", BASIC)
        val expectedResponse = Json.obj("key" -> "value")
        withRestService(expectedResponse) {
          verifyRequest(_, "POST", url, auth = Some(authConfig), body = Some(body))
        } { client =>
          whenReady(client.post(url, body, auth = Some(authConfig))) { response =>
            response.json mustEqual expectedResponse
          }
        }
      }
    }
  }

  def withServer[T](
      routes: BuiltInComponents => PartialFunction[RequestHeader, Handler]
  )(test: RestClient => T): T = Server.withRouterFromComponents()(routes) { implicit port =>
    withClient { client =>
      test(new RestClient(client))
    }
  }

  def withRestService[R, T](response: R)(
      assertRequest: Request[Option[JsValue]] => Assertion = _ => succeed
  )(test: RestClient => T)(implicit writeable: Writeable[R]): T = {
    withServer { components =>
      import components.{defaultActionBuilder => Action}
      import play.api.mvc.Results._
      { case requestHeader: RequestHeader =>
        Action { request =>
          assertRequest(requestHeader.withBody(request.body.asJson))
          Ok(response)
        }
      }
    }(test)
  }

  def verifyRequest(
      request: Request[Option[JsValue]],
      method: String,
      url: String,
      headers: Map[String, String] = Map.empty,
      queryParams: Map[String, String] = Map.empty,
      auth: Option[AuthConfig] = None,
      body: Option[JsValue] = None
  ): Assertion = {
    request.method mustBe method
    request.uri mustEqual url
    request.headers.toSimpleMap.toSeq must contain allElementsOf headers.toSeq
    queryParams.toSeq must contain allElementsOf queryParams.toSeq

    auth match {
      case Some(config) =>
        request.headers.hasHeader(AUTHORIZATION) mustBe true
        val authDigest =
          Base64.getEncoder.encodeToString(s"${config.username}:${config.password}".getBytes)
        request.headers.get(AUTHORIZATION) mustBe Some(s"Basic $authDigest")
      case _ =>
    }

    body match {
      case Some(value) =>
        request.hasBody mustBe true
        request.body mustBe Some(value)
      case _ => succeed
    }
  }

  implicit lazy val serverProvider: ServerProvider = {
    val settings = System.getProperties
    settings.put("play.server.provider", "play.core.server.AkkaHttpServerProvider")
    val classLoader = this.getClass.getClassLoader
    val config = Configuration
      .load(
        classLoader,
        settings,
        Map.empty,
        allowMissingApplicationConf = true
      )
      .withFallback(
        new api.Configuration(
          ConfigFactory.parseString("play.server.provider:play.core.server.AkkaHttpServerProvider")
        )
      )
    fromConfiguration(classLoader, config)
  }

}
