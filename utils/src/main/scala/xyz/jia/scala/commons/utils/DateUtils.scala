package xyz.jia.scala.commons.utils

import java.time._

trait DateUtils {

  def switchTimeZones(
      date: LocalDateTime,
      fromZone: ZoneId,
      toZone: ZoneId = ZoneOffset.UTC
  ): LocalDateTime =
    date.atZone(fromZone).toOffsetDateTime.atZoneSameInstant(toZone).toLocalDateTime

  def toLocalDateTime(
      offsetDateTime: OffsetDateTime,
      zoneId: ZoneId = ZoneOffset.UTC
  ): LocalDateTime = offsetDateTime.atZoneSameInstant(zoneId).toLocalDateTime

  def nowAtZone(zoneId: ZoneId = ZoneOffset.UTC): LocalDateTime =
    LocalDateTime.now(zoneId)

  def startOfMonth(
      date: LocalDate,
      currentMonthOffset: Long
  ): LocalDateTime = date.atStartOfDay().plusMonths(currentMonthOffset).withDayOfMonth(1)

  /** Gets the start of a given month relative to the current month
    * @param zoneId
    *   The Zone in which to perform the date computations
    * @param currentMonthOffset
    *   the number of months relative to the current month for which the beginning is desired.
    *   Default is 0 which means we want the beginning of the current month.
    *
    * @return
    *   the beginning of the desired month
    */
  def startOfMonth(
      zoneId: ZoneId = ZoneOffset.UTC,
      currentMonthOffset: Long = 0
  ): LocalDateTime = startOfMonth(nowAtZone(zoneId).toLocalDate, currentMonthOffset)

  def endOfMonth(
      date: LocalDate,
      currentMonthOffset: Long
  ): LocalDateTime = {
    val beginning = startOfMonth(date, currentMonthOffset).toLocalDate
    LocalDateTime.of(beginning, LocalTime.MAX).plusMonths(1).minusDays(1)
  }

  /** Gets the end of a given month relative to the current month
    * @param zoneId
    *   The Zone in which to perform the date computations
    * @param currentMonthOffset
    *   the number of months relative to the current month for which the beginning is desired.
    *   Default is 0 which means we want the end of the current month.
    *
    * @return
    *   the end of the desired month
    */
  def endOfMonth(
      zoneId: ZoneId = ZoneOffset.UTC,
      currentMonthOffset: Long = 0
  ): LocalDateTime = endOfMonth(nowAtZone(zoneId).toLocalDate, currentMonthOffset)

}

object DateUtils extends DateUtils
