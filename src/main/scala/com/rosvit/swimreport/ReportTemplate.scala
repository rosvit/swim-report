package com.rosvit.swimreport

import com.rosvit.swimreport.fit.SwimStroke
import io.circe.Printer
import io.circe.syntax.*

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.{LocalDateTime, ZoneOffset}

object ReportTemplate {

  def text(sr: SwimReport): String =
    s"""|${Console.BOLD}${Console.BLUE}SWIM REPORT${Console.RESET}
        |Pool length:     ${sr.poolLength} m
        |Distance:        ${sr.distance} m
        |Lengths (total): ${sr.lengthCount}
        |Average pace:    ${sr.avgPace} / 100m
        |Swimming time:   ${sr.duration}
        |Resting time:    ${sr.rest}
        |Start time:      ${formatStartTime(sr.startTime, sr.utcOffsetSecs)}
        |Average HR:      ${if (sr.avgHr > 0) sr.avgHr else "N/A"} bpm
        |${strokesTemplate(sr.summary)}""".stripMargin

  private def strokesTemplate(summaryMap: Map[SwimStroke, SwimStrokeSummary]): String =
    summaryMap.toList
      .sortBy(_._1.toString)
      .map { case (stroke, summary) =>
        s"""|
            |${Console.BOLD}${Console.GREEN}${stroke.toString.toUpperCase}${Console.RESET}
            |Lengths:          ${summary.lengthCount}
            |Distance:         ${summary.distance} m
            |Average pace:     ${summary.avgPace} / 100m
            |Longest interval: ${summary.longestInterval} m
            |Average strokes:  ${summary.strokeCount} strokes / length
            |Stroke rate:      ${summary.strokeRate} strokes / minute
            |SWOLF:            ${summary.swolf}""".stripMargin
      }
      .mkString("\n")

  private def formatStartTime(utc: java.time.Instant, offset: Int): String =
    LocalDateTime
      .ofInstant(utc, ZoneOffset.ofTotalSeconds(offset))
      .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM))

  def jsonString(sr: SwimReport): String = sr.asJson.printWith(Printer.spaces2)
}
