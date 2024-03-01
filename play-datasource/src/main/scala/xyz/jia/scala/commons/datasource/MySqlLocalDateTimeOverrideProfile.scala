package xyz.jia.scala.commons.datasource

import java.sql.PreparedStatement
import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField

import slick.ast.FieldSymbol
import slick.jdbc.{GetResult, SetParameter}

/** Allows the use of `DATETIME` data type for storing `java.time.LocalDateTime`. The default Slick
  * expectation is that the database type should be TEXT — which is not ideal. Source
  * https://scala-slick.org/doc/3.3.3/upgrade.html#slick.jdbc.mysqlprofile <p></p>
  *
  * This profile maps `java.time.LocalDateTime` to text to ensure that fractional seconds are not
  * lost when writing to the database.
  */
trait MySqlLocalDateTimeOverrideProfile
    extends slick.jdbc.JdbcProfile
    with slick.jdbc.MySQLProfile {

  import java.sql.ResultSet

  override val columnTypes = new JdbcTypesOverride

  val formatter: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd HH:mm:ss")
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
      .toFormatter

  implicit def setParameterLocalDateTime: SetParameter[LocalDateTime] =
    SetParameter { case (value, pp) => pp.setString(value.format(formatter)) }

  implicit def getParameterLocalDateTime: GetResult[LocalDateTime] =
    GetResult(r => LocalDateTime.parse(r.nextString(), formatter))

  class JdbcTypesOverride extends super.JdbcTypes {

    override val localDateTimeType: LocalDateTimeJdbcType = new LocalDateTimeJdbcType {

      override def sqlTypeName(sym: Option[FieldSymbol]) = "DATETIME(6)"

      override def setValue(value: LocalDateTime, statement: PreparedStatement, idx: Int): Unit =
        statement.setString(idx, value.format(formatter))

      override def getValue(resultSet: ResultSet, idx: Int): LocalDateTime = {
        resultSet.getString(idx) match {
          case null      => null
          case timestamp => LocalDateTime.parse(timestamp, formatter)
        }
      }

      override def updateValue(value: LocalDateTime, resultSet: ResultSet, idx: Int): Unit =
        resultSet.updateString(idx, if (value == null) null else value.format(formatter))

      override def valueToSQLLiteral(value: LocalDateTime) = s"'${value.format(formatter)}'"

    }

  }

}

object MySqlLocalDateTimeOverrideProfile extends MySqlLocalDateTimeOverrideProfile
