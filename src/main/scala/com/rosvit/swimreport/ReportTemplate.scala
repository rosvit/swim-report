package com.rosvit.swimreport

import com.rosvit.swimreport.fit.SwimStroke
import io.circe.Printer
import io.circe.syntax.*

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.{LocalDateTime, ZoneOffset}

object ReportTemplate {

  def text(sr: SwimReport): String =
    s"""|SWIM REPORT
        |
        |Pool length:     ${sr.poolLength} m
        |Distance:        ${sr.distance} m
        |Lengths (total): ${sr.lengthCount}
        |Swimming time:   ${sr.duration}
        |Resting time:    ${sr.rest}
        |Start time:      ${formatStartTime(sr.startTime, sr.utcOffsetSecs)}
        |Avg. HR:         ${sr.avgHr} bpm
        |${strokesTemplate(sr.summary)}""".stripMargin

  private def strokesTemplate(summaryMap: Map[SwimStroke, SwimStrokeSummary]): String =
    summaryMap
      .map { case (stroke, summary) =>
        s"""|
            |${stroke.toString.toUpperCase}
            |Lengths:          ${summary.lengthCount}
            |Distance:         ${summary.distance} m
            |Average pace:     ${summary.avgPace} / 100m
            |Longest interval: ${summary.longestInterval} m""".stripMargin
      }
      .mkString("\n")

  private def formatStartTime(utc: java.time.Instant, offset: Int): String =
    LocalDateTime
      .ofInstant(utc, ZoneOffset.ofTotalSeconds(offset))
      .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM))

  def jsonString(sr: SwimReport): String = sr.asJson.printWith(Printer.spaces2)
}
