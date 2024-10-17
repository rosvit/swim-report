package com.rosvit.swimreport

import cats.Functor
import cats.effect.Sync
import cats.implicits.*
import com.rosvit.swimreport.fit.*
import fs2.Stream

trait ReportProcessor[F[_]] {
  def process(input: Stream[F, FitMessage]): F[Option[SwimReport]]
}

object ReportProcessor {

  def make[F[_]: Sync: Functor]: F[ReportProcessor[F]] = Sync[F].delay { (messages: Stream[F, FitMessage]) =>
    messages
      .fold[Option[Activity]](None) { (acc, msg) =>
        msg match {
          case msg: SessionMessage  => processSessionMessage(acc, msg)
          case msg: LapMessage      => processLapMessage(acc, msg)
          case msg: LengthMessage   => processLengthMessage(acc, msg)
          case msg: ActivityMessage => processActivityMessage(acc, msg)
        }
      }
      .compile
      .last
      .map(_.flatten.map(toReport))
  }

  private def processSessionMessage(maybeActivity: Option[Activity], msg: SessionMessage): Option[Activity] =
    (maybeActivity, msg.sport) match {
      case (Some(activityData), Sport.Swimming) =>
        Some(
          activityData.copy(
            poolLength = msg.poolLengthMeters,
            distance = msg.distance,
            duration = msg.timerTime,
            startTime = msg.startTime,
            avgHr = msg.avgHr
          )
        )
      case (None, Sport.Swimming) => Some(Activity(msg))
      case _                      => None
    }

  private def processLapMessage(maybeActivity: Option[Activity], msg: LapMessage): Option[Activity] =
    maybeActivity.orElse(Some(Activity.empty)).map { activity =>
      if (msg.isRest) {
        activity.copy(rests = activity.rests :+ Rest(msg.timerTime))
      } else {
        activity.copy(laps = activity.laps :+ Lap(msg))
      }
    }

  private def processLengthMessage(maybeActivity: Option[Activity], msg: LengthMessage): Option[Activity] =
    maybeActivity.orElse(Some(Activity.empty)).map { activity =>
      // we are interested only in active lengths
      if (msg.active) activity.copy(lengths = activity.lengths :+ Length(msg.swimStroke, msg.timerTime)) else activity
    }

  private def processActivityMessage(maybeActivity: Option[Activity], msg: ActivityMessage): Option[Activity] =
    maybeActivity.orElse(Some(Activity.empty)).map { activity =>
      val offset = msg.localTimestamp - msg.timestamp
      activity.copy(utcOffsetSecs = offset.toInt)
    }

  private def toReport(activity: Activity): SwimReport =
    SwimReport(
      poolLength = activity.poolLength,
      distance = activity.distance,
      lengthCount = activity.lengths.size,
      duration = FormattedDuration(activity.duration),
      startTime = activity.startTime,
      utcOffsetSecs = activity.utcOffsetSecs,
      avgHr = activity.avgHr,
      rest = FormattedDuration(activity.rests.map(_.duration).sum),
      summary = strokeSummary(activity)
    )

  private def strokeSummary(activity: Activity): Map[SwimStroke, SwimStrokeSummary] = {
    val lapsByStroke = activity.laps.groupBy(_.swimStroke)
    val lengthsByStroke = activity.lengths.groupBy(_.swimStroke)

    lengthsByStroke.map { case (stroke, lengths) =>
      val longest = lapsByStroke
        .get(stroke)
        .flatMap(_.sortBy(_.distance)(using Ordering[Float].reverse).headOption)
        .map(_.distance)
        .getOrElse(activity.poolLength)
      val distance = activity.poolLength * lengths.size
      val pace = lengths.map(_.duration).sum / (distance / 100f)
      stroke -> SwimStrokeSummary(lengths.size, distance, longest, FormattedDuration.pace(pace))
    }
  }
}
