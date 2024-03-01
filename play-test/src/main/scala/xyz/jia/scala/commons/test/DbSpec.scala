package xyz.jia.scala.commons.test

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.db.slick.{DatabaseConfigProvider, DbName, SlickComponents}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{ApplicationLifecycle, Injector}
import play.api.{Application, Configuration, Environment, Play}
import slick.basic.{BasicProfile, DatabaseConfig}

trait DbSpec extends Suite with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit lazy val application: Application =
    new GuiceApplicationBuilder().loadConfig(env => Configuration.load(env)).build()

  lazy val injector: Injector = application.injector

  lazy val databaseApi: DBApi = injector.instanceOf[DBApi]

  val databaseName: String

  val dbConfigProvider: DatabaseConfigProvider = new DatabaseConfigProvider with SlickComponents {

    override def environment: Environment = application.environment

    override def configuration: Configuration = application.configuration

    override def applicationLifecycle: ApplicationLifecycle =
      injector.instanceOf[ApplicationLifecycle]

    override def executionContext: ExecutionContext = global

    override def get[P <: BasicProfile]: DatabaseConfig[P] = slickApi.dbConfig(DbName(databaseName))

  }

  override def beforeEach(): Unit = Evolutions.applyEvolutions(databaseApi.database(databaseName))

  override def afterAll(): Unit = {
    super.afterAll()
    Play.stop(application)
  }

  override def afterEach(): Unit =
    Evolutions.cleanupEvolutions(databaseApi.database(databaseName))

}
