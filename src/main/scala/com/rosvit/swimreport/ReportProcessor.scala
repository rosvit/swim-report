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
      .filter {
        case LengthMessage(_, _, _, active) => active // we are interested only in active lengths
        case _                              => true
      }
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
    activityOrDefault(maybeActivity).map { activity =>
      if (msg.isRest) {
        activity.copy(rests = activity.rests :+ Rest(msg.timerTime))
      } else {
        activity.copy(laps = activity.laps :+ Lap(msg))
      }
    }

  private def processLengthMessage(maybeActivity: Option[Activity], msg: LengthMessage): Option[Activity] =
    activityOrDefault(maybeActivity).map { activity =>
      activity.copy(lengths = activity.lengths :+ Length(msg.swimStroke, msg.timerTime, msg.index))
    }

  private def processActivityMessage(maybeActivity: Option[Activity], msg: ActivityMessage): Option[Activity] =
    activityOrDefault(maybeActivity).map { activity =>
      val offset = msg.localTimestamp - msg.timestamp
      activity.copy(utcOffsetSecs = offset.toInt)
    }

  private def activityOrDefault(maybeActivity: Option[Activity]): Option[Activity] =
    maybeActivity.orElse(Some(Activity.empty))

  private def toReport(activity: Activity): SwimReport = {
    val movingTime = activity.lengths.map(_.duration).sum
    SwimReport(
      poolLength = activity.poolLength,
      distance = activity.distance,
      lengthCount = activity.lengths.size,
      duration = FormattedDuration(movingTime),
      startTime = activity.startTime,
      utcOffsetSecs = activity.utcOffsetSecs,
      avgPace = FormattedDuration.pace(movingTime / (activity.distance / 100f)),
      avgHr = activity.avgHr,
      rest = FormattedDuration(activity.rests.map(_.duration).sum),
      summary = strokeSummary(activity)
    )
  }

  private def strokeSummary(activity: Activity): Map[SwimStroke, SwimStrokeSummary] = {
    val detectedIntervals = detectLongestIntervals(activity.laps, activity.lengths.sortBy(_.index))
    activity.lengths.groupBy(_.swimStroke).map { case (stroke, lengths) =>
      val longest = detectedIntervals.get(stroke).map(_ * activity.poolLength).getOrElse(activity.poolLength)
      val distance = activity.poolLength * lengths.size
      val pace = lengths.map(_.duration).sum / (distance / 100f)
      stroke -> SwimStrokeSummary(lengths.size, distance, longest, FormattedDuration.pace(pace))
    }
  }

  private def detectLongestIntervals(laps: List[Lap], lengths: List[Length]): Map[SwimStroke, Int] =
    laps.foldLeft(Map.empty[SwimStroke, Int]) { (longest, lap) =>
      (lap.swimStroke, lap.lengthCount) match {
        case (_, 0) => longest
        case (SwimStroke.Mixed | SwimStroke.Other, lengthCount) =>
          val detected = detectInLapLengths(lengths.dropWhile(_.index < lap.firstLengthIndex).take(lengthCount))
            .filterNot((intStroke, intLengths) => longest.get(intStroke).exists(_ >= intLengths))
          longest ++ detected
        case (stroke, lengthCount) => longest.updatedWith(stroke)(currentOr(lengthCount))
      }
    }

  private def detectInLapLengths(lengths: List[Length]): Map[SwimStroke, Int] =
    lengths
      .foldLeft(Map.empty[SwimStroke, Int] -> Option.empty[Interval]) { case ((longest, current), length) =>
        if (current.exists(_.swimStroke == length.swimStroke)) {
          val newCount = current.map(_.lengthCount + 1).getOrElse(1)
          longest.updatedWith(length.swimStroke)(currentOr(newCount)) -> Some(Interval(length.swimStroke, newCount))
        } else {
          longest.updatedWith(length.swimStroke)(currentOr(1)) -> Some(Interval(length.swimStroke, 1))
        }
      }
      ._1

  private def currentOr(value: Int): Option[Int] => Option[Int] = {
    case Some(currentMax) if currentMax >= value => Some(currentMax)
    case _                                       => Some(value)
  }
}
