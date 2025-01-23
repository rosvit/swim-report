package com.rosvit.swimreport.fit

import com.garmin.fit.{ActivityMesg, LapMesg, LengthMesg, LengthType, SessionMesg}

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
      lengths = Option(mesg.getNumActiveLengths).getOrElse(mesg.getNumLengths),
      timerTime = mesg.getTotalTimerTime,
      avgHr = mesg.getAvgHeartRate,
      firstLengthIndex = mesg.getFirstLengthIndex
    )
}

extension (mesg: LengthMesg) {
  def toDomain: LengthMessage =
    LengthMessage(
      swimStroke = SwimStroke.fromFitValue(mesg.getSwimStroke),
      timerTime = mesg.getTotalTimerTime,
      index = mesg.getMessageIndex,
      active = mesg.getLengthType == LengthType.ACTIVE
    )
}

extension (mesg: ActivityMesg) {
  def toDomain: ActivityMessage =
    ActivityMessage(timestamp = mesg.getTimestamp.getTimestamp, localTimestamp = mesg.getLocalTimestamp)
}
