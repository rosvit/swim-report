package com.rosvit.swimreport.fit

import com.garmin.fit

import java.time.Instant

enum FitFileStatus(val message: String) {
  case Ok extends FitFileStatus("OK")
  case NotExist extends FitFileStatus("File not found")
  case NotFitFile extends FitFileStatus("File is not a FIT file")
  case IntegrityCheckFailed
      extends FitFileStatus(
        "Integrity check of the FIT file failed, try using --ignore-integrity flag to ignore it"
      )
  case UnexpectedError extends FitFileStatus("Unexpected error while reading FIT file")
}

enum Sport {
  case Swimming, Other
}

object Sport {
  def fromFitValue(value: fit.Sport): Sport = value match {
    case fit.Sport.SWIMMING => Swimming
    case _                  => Other
  }
}

enum SwimStroke {
  case Freestyle, Breaststroke, Backstroke, Butterfly, Mixed, Other
}

object SwimStroke {
  def fromFitValue(value: fit.SwimStroke): SwimStroke = value match {
    case fit.SwimStroke.FREESTYLE    => Freestyle
    case fit.SwimStroke.BREASTSTROKE => Breaststroke
    case fit.SwimStroke.BACKSTROKE   => Backstroke
    case fit.SwimStroke.BUTTERFLY    => Butterfly
    case fit.SwimStroke.MIXED | null => Mixed // Coros FIT files contain null instead MIXED for mixed laps
    case _                           => Other
  }
}

sealed trait FitMessage

final case class SessionMessage(
    sport: Sport,
    poolLengthMeters: Float,
    distance: Float,
    timerTime: Float,
    startTime: Instant,
    avgHr: Short
) extends FitMessage

final case class LapMessage(swimStroke: SwimStroke, distance: Float, lengths: Int, timerTime: Float, avgHr: Short)
    extends FitMessage {
  def isRest: Boolean = lengths == 0 && (swimStroke == SwimStroke.Other || swimStroke == SwimStroke.Mixed)
}

final case class LengthMessage(swimStroke: SwimStroke, timerTime: Float, active: Boolean = true) extends FitMessage

final case class ActivityMessage(timestamp: Long, localTimestamp: Long) extends FitMessage
