package com.rosvit.swimreport

import com.rosvit.swimreport.fit.{LapMessage, SessionMessage, SwimStroke}

import java.time.Instant

final case class Activity(
    poolLength: Float,
    distance: Float,
    duration: Float,
    startTime: Instant,
    avgHr: Short,
    lengths: List[Length],
    laps: List[Lap],
    rests: List[Rest]
)

object Activity {

  def empty: Activity = Activity(0f, 0f, 0f, Instant.now, 0, Nil, Nil, Nil)

  def apply(msg: SessionMessage): Activity = Activity(
    poolLength = msg.poolLengthMeters,
    distance = msg.distance,
    duration = msg.timerTime,
    startTime = msg.startTime,
    avgHr = msg.avgHr,
    lengths = Nil,
    laps = Nil,
    rests = Nil
  )
}

final case class Lap(swimStroke: SwimStroke, distance: Float, lengthCount: Int, duration: Float, avgHr: Short)

object Lap {

  def apply(msg: LapMessage): Lap = Lap(
    swimStroke = msg.swimStroke,
    distance = msg.distance,
    lengthCount = msg.lengths,
    duration = msg.timerTime,
    avgHr = msg.avgHr
  )
}

final case class Rest(duration: Float)

final case class Length(swimStroke: SwimStroke, duration: Float)
