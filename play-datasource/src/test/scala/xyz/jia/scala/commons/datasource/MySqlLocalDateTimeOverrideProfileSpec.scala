package xyz.jia.scala.commons.datasource

import java.time.LocalDateTime

import scala.annotation.nowarn
import scala.concurrent.Future

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.{GetResult, JdbcProfile, SetParameter}
import slick.lifted.ProvenShape

import xyz.jia.scala.commons.test.DbSpec

class MySqlLocalDateTimeOverrideProfileSpec
    extends HasDatabaseConfigProvider[JdbcProfile]
    with DbSpec
    with Matchers
    with ScalaFutures
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  import profile.api._

  override val databaseName: String = "default"

  private val sampleDao: SampleDao = new SampleDao(dbConfigProvider)

  "MySqlLocalDataTimeOverrideProfile" should {
    "generate the correct create table SQL" in {
      sampleDao.createTableStatements.mkString mustEqual "create table " +
        "`my_sql_local_data_time_override_profile_tests` " +
        "(`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`created_at` DATETIME(6) NOT NULL," +
        "`updated_at` DATETIME(6) NOT NULL)"
    }

    "correctly serialize and deserialize LocalDateTime values to the DB" in {
      val createdAt = LocalDateTime.parse("2022-01-25T14:04:42.544082")
      // updatedAt has fractional seconds greater than what MySQL supports i.e 6 on purpose
      val updatedAt = LocalDateTime.parse("2022-03-31T23:59:59.999999999")
      whenReady(sampleDao.insert(Sample(None, createdAt, updatedAt))) { id: Long =>
        id > 0 mustBe true
        // verify that can read back saved value
        whenReady(sampleDao.findById(id)) { result =>
          result.isDefined mustBe true
          result.get must have(
            Symbol("id")(Some(id)),
            Symbol("createdAt")(createdAt),
            // date must be truncated as desired
            Symbol("updatedAt")(LocalDateTime.parse("2022-03-31T23:59:59.999999"))
          )

          // verify that can update values
          val updatedCreatedAt = createdAt.plusMinutes(18)
          val updatedUpdatedAt = updatedAt.plusHours(1)
          val update = result.get.copy(createdAt = updatedCreatedAt, updatedAt = updatedUpdatedAt)
          whenReady(sampleDao.update(update)) { updateCount =>
            updateCount mustEqual 1
            // verify that can read back updated values
            whenReady(sampleDao.findById(id)) { updated =>
              updated.isDefined mustBe true
              updated.get must have(
                Symbol("id")(Some(id)),
                Symbol("createdAt")(updatedCreatedAt),
                // date must be truncated as desired
                Symbol("updatedAt")(LocalDateTime.parse("2022-04-01T00:59:59.999999"))
              )
            }
          }
        }
      }
    }

  }

  "setParameterLocalDateTime" should {
    "correctly set a date parameter value" in {
      @nowarn implicit def dateWriter: SetParameter[LocalDateTime] =
        MySqlLocalDateTimeOverrideProfile.setParameterLocalDateTime

      val createdAt = LocalDateTime.parse("2022-01-25T14:04:42.544082")
      // updatedAt has fractional seconds greater than what MySQL supports i.e 6
      val updatedAt = LocalDateTime.parse("2022-03-31T23:59:59.999999999")
      whenReady(sampleDao.insert(Sample(None, createdAt, updatedAt))) { id =>
        withClue("test a date with fractional seconds greater then what MySQL supports") {
          val query =
            sql""" 
                 SELECT id FROM my_sql_local_data_time_override_profile_tests 
                 WHERE updated_at = $updatedAt and id = $id
            """.as[Long]

          whenReady(db.run(query))(_ mustEqual Seq(id))
        }
      }
    }
  }

  "getParameterLocalDateTime" should {
    "get a date parameter value" in {

      implicit def dateReader: GetResult[LocalDateTime] =
        MySqlLocalDateTimeOverrideProfile.getParameterLocalDateTime

      val createdAt = LocalDateTime.parse("2022-01-25T14:04:42.544082")
      // updatedAt has fractional seconds greater than what MySQL supports i.e 6 on purpose
      val updatedAt = LocalDateTime.parse("2022-03-31T23:59:59.999999999")
      whenReady(sampleDao.insert(Sample(None, createdAt, updatedAt))) { id =>
        val query =
          sql""" 
               SELECT updated_at FROM my_sql_local_data_time_override_profile_tests WHERE id = $id
             """.as[LocalDateTime]

        whenReady(db.run(query))(
          _ mustEqual Seq(LocalDateTime.parse("2022-03-31T23:59:59.999999"))
        )
      }
    }
  }

  case class Sample(id: Option[Long], createdAt: LocalDateTime, updatedAt: LocalDateTime)

  class SampleDao(protected val dbConfigProvider: DatabaseConfigProvider)
      extends HasDatabaseConfigProvider[JdbcProfile] {

    import profile.api._

    class SampleTable(tag: Tag)
        extends Table[Sample](tag, "my_sql_local_data_time_override_profile_tests") {

      import MySqlLocalDateTimeOverrideProfile.api._

      def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

      def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")

      def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

      override def * : ProvenShape[Sample] =
        (id.?, createdAt, updatedAt) <> (Sample.tupled, Sample.unapply)

    }

    lazy val samples = TableQuery[SampleTable]

    def insert(sample: Sample): Future[Long] =
      db.run(samples returning samples.map(_.id) += sample)

    def findById(sampleId: Long): Future[Option[Sample]] =
      db.run(samples.filter(_.id === sampleId).result.headOption)

    def update(sample: Sample): Future[Int] =
      db.run(samples.filter(_.id === sample.id).update(sample))

    def createTableStatements: Iterable[String] = samples.schema.create.statements

  }

}
