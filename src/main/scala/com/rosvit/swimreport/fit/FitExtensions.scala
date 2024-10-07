package com.rosvit.swimreport.fit

import com.garmin.fit.{LapMesg, LengthMesg, SessionMesg}

extension (mesg: SessionMesg) {
  def toDomain: SessionMessage =
    SessionMessage(
      sport = Sport.fromFitValue(mesg.getSport),
      poolLengthMeters = mesg.getPoolLength,
      distance = mesg.getTotalDistance,
      timerTime = mesg.getTotalTimerTime,
      startTime = mesg.getStartTime.getDate.toInstant,
      avgHr = mesg.getAvgHeartRate
    )
}

extension (mesg: LapMesg) {
  def toDomain: LapMessage =
    LapMessage(
      swimStroke = SwimStroke.fromFitValue(mesg.getSwimStroke),
      distance = mesg.getTotalDistance,
      lengths = mesg.getNumLengths,
      timerTime = mesg.getTotalTimerTime,
      avgHr = mesg.getAvgHeartRate
    )
}

extension (mesg: LengthMesg) {
  def toDomain: LengthMessage =
    LengthMessage(swimStroke = SwimStroke.fromFitValue(mesg.getSwimStroke), timerTime = mesg.getTotalTimerTime)
}
