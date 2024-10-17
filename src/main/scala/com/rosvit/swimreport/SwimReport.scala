package com.rosvit.swimreport

import com.rosvit.swimreport.fit.SwimStroke
import io.circe.generic.semiauto.*
import io.circe.{Encoder, KeyEncoder}

import java.time.{Duration, Instant}

opaque type FormattedDuration = String

object FormattedDuration {
  given (using ev: Encoder[String]): Encoder[FormattedDuration] = ev

  def apply(seconds: Float): FormattedDuration = {
    val d = getDuration(seconds)
    String.format("%02d:%02d:%02d", d.toHours, d.toMinutesPart, d.toSecondsPart)
  }

  def pace(seconds: Float): FormattedDuration = {
    val d = getDuration(seconds)
    String.format("%02d:%02d", d.toMinutes, d.toSecondsPart)
  }

  private def getDuration(seconds: Float): Duration = Duration.ofMillis((seconds * 1000).toLong)
}

final case class SwimStrokeSummary(
    lengthCount: Int,
    distance: Float,
    longestInterval: Float,
    avgPace: FormattedDuration
)

object SwimStrokeSummary {
  given Encoder[SwimStrokeSummary] = deriveEncoder
}

final case class SwimReport(
    poolLength: Float,
    distance: Float,
    lengthCount: Int,
    duration: FormattedDuration,
    startTime: Instant,
    utcOffsetSecs: Int,
    avgHr: Int,
    rest: FormattedDuration,
    summary: Map[SwimStroke, SwimStrokeSummary]
)

object SwimReport {
  given KeyEncoder[SwimStroke] = (key: SwimStroke) => key.toString
  given Encoder[SwimReport] = deriveEncoder
}
