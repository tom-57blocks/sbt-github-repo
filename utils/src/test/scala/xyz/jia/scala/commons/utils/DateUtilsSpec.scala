package xyz.jia.scala.commons.utils

import java.time._

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, spy}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DateUtilsSpec extends AnyWordSpec with Matchers {

  "switchTimeZones" should {
    "correctly convert from one date to another" in {
      DateUtils
        .switchTimeZones(LocalDateTime.parse("2022-02-18T16:36:22.001"), ZoneId.of("UTC+3"))
        .toString mustEqual LocalDateTime.parse("2022-02-18T13:36:22.001").toString

      DateUtils
        .switchTimeZones(
          LocalDateTime.parse("2022-02-18T16:36:22.001"),
          ZoneId.of("UTC+2"),
          ZoneId.of("UTC+3")
        )
        .toString mustEqual LocalDateTime.parse("2022-02-18T17:36:22.001").toString
    }
  }

  "toLocalDateTime" should {
    val offsetDateTime = OffsetDateTime.parse("2022-02-18T16:36:22.001+03:00")

    "correctly convert an offset date to UTC by default" in {
      DateUtils
        .toLocalDateTime(offsetDateTime)
        .toString mustEqual LocalDateTime.parse("2022-02-18T13:36:22.001").toString
    }

    "correctly convert an offset date to the a specified timezone" in {
      DateUtils
        .toLocalDateTime(offsetDateTime, ZoneId.of("UTC+2"))
        .toString mustEqual LocalDateTime.parse("2022-02-18T15:36:22.001").toString
    }
  }

  "nowAtZone" should {
    "return the current date time at UTC by default" in {
      DateUtils.nowAtZone().withNano(0).toString mustEqual
        LocalDateTime.now(ZoneOffset.UTC).withNano(0).toString
    }

    "return the current date time at the specified timezone" in {
      DateUtils.nowAtZone(ZoneId.of("UTC-1")).withNano(0).toString mustEqual
        LocalDateTime.now(ZoneId.of("UTC-1")).withNano(0).toString
    }
  }

  "startOfMonth" when {
    class DateUtilsInstance extends DateUtils

    "given a ZoneId" should {
      "correctly return the first date of a month relative to the current month" in {
        val spyDateUtils: DateUtils = spy(new DateUtilsInstance)

        val testDate: LocalDateTime = LocalDateTime.parse("2022-04-07T05:42:45.926598")
        doReturn(testDate).when(spyDateUtils).nowAtZone(any[ZoneOffset]())

        spyDateUtils.startOfMonth() mustEqual LocalDateTime.parse("2022-04-01T00:00")
        spyDateUtils.startOfMonth(currentMonthOffset = 2) mustEqual
          LocalDateTime.parse("2022-06-01T00:00")
        spyDateUtils.startOfMonth(currentMonthOffset = -1) mustEqual
          LocalDateTime.parse("2022-03-01T00:00")

      }
    }

    "given a LocalDate" should {
      "correctly return the first date of a month relative to the current month" in {
        val testDate: LocalDate = LocalDate.parse("2022-04-07")
        val dateUtils: DateUtils = new DateUtilsInstance
        dateUtils.startOfMonth(testDate, 0) mustEqual LocalDateTime.parse("2022-04-01T00:00")
        dateUtils.startOfMonth(testDate, 2) mustEqual LocalDateTime.parse("2022-06-01T00:00")
        dateUtils.startOfMonth(testDate, -1) mustEqual LocalDateTime.parse("2022-03-01T00:00")
      }
    }
  }

  "endOfMonth" when {
    class DateUtilsInstance extends DateUtils
    "given a ZoneId" should {
      "correctly return the last date of a month relative to the current month" in {
        val spyDateUtils: DateUtils = spy(new DateUtilsInstance)
        val testDate: LocalDateTime = LocalDateTime.parse("2022-04-07T05:42:45.926598")
        doReturn(testDate).when(spyDateUtils).nowAtZone(any[ZoneOffset]())

        withClue("month ending on 30th") {
          spyDateUtils.endOfMonth() mustEqual LocalDateTime.parse("2022-04-30T23:59:59.999999999")
        }

        withClue("month ending on 31st") {
          spyDateUtils.endOfMonth(currentMonthOffset = 1) mustEqual
            LocalDateTime.parse("2022-05-31T23:59:59.999999999")
        }

        withClue("month of February for a non-leap year") {
          spyDateUtils.endOfMonth(currentMonthOffset = -2) mustEqual
            LocalDateTime.parse("2022-02-28T23:59:59.999999999")
        }

        withClue("month of February for a leap year") {
          spyDateUtils.endOfMonth(currentMonthOffset = 22) mustEqual
            LocalDateTime.parse("2024-02-29T23:59:59.999999999")
        }
      }
    }

    "given a LocalDate" should {
      "correctly return the last date of a month relative to the current month" in {
        val spyDateUtils: DateUtils = spy(new DateUtilsInstance)
        val testDate: LocalDate = LocalDate.parse("2022-04-07")
        withClue("month ending on 30th") {
          spyDateUtils.endOfMonth(testDate, 0) mustEqual
            LocalDateTime.parse("2022-04-30T23:59:59.999999999")
        }

        withClue("month ending on 31st") {
          spyDateUtils.endOfMonth(testDate, 1) mustEqual
            LocalDateTime.parse("2022-05-31T23:59:59.999999999")
        }

        withClue("month of February for a non-leap year") {
          spyDateUtils.endOfMonth(testDate, -2) mustEqual
            LocalDateTime.parse("2022-02-28T23:59:59.999999999")
        }

        withClue("month of February for a leap year") {
          spyDateUtils.endOfMonth(testDate, 22) mustEqual
            LocalDateTime.parse("2024-02-29T23:59:59.999999999")
        }
      }
    }
  }

}
