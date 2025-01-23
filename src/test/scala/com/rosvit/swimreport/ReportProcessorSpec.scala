package com.rosvit.swimreport

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rosvit.swimreport.fit.*
import com.rosvit.swimreport.fit.SwimStroke.*
import fs2.Stream
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, OptionValues}

import java.time.Instant
import scala.concurrent.duration.given

class ReportProcessorSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers with OptionValues {

  "ReportProcessor" should "not produce report if sport is not swimming" in withProcessor { processor =>
    val stream: Stream[IO, FitMessage] = Stream.emit(SessionMessage(Sport.Other, 0f, 0f, 0f, Instant.MIN, 0))
    processor.process(stream).asserting(_ should be(empty))
  }

  it should "handle empty stream" in withProcessor { processor =>
    val stream: Stream[IO, FitMessage] = Stream.empty
    processor.process(stream).asserting(_ should be(empty))
  }

  it should "produce swim report" in withProcessor { processor =>
    val startTime = Instant.now()
    val messages: List[FitMessage] = List(
      LengthMessage(Freestyle, 25f, 0),
      LengthMessage(Freestyle, 35f, 1),
      LengthMessage(Breaststroke, 30f, 2),
      LengthMessage(Breaststroke, 50f, 3),
      LengthMessage(Breaststroke, 40f, 4),
      LengthMessage(Freestyle, 30f, 5),
      LapMessage(Freestyle, 50f, 2, 0, 60f, 120),
      LapMessage(Breaststroke, 75f, 3, 2, 120f, 120),
      LapMessage(Freestyle, 25f, 1, 5, 60f, 120),
      LapMessage(Mixed, 0f, 0, 5, 100f, 110),
      SessionMessage(Sport.Swimming, 25f, 150f, 240f, startTime, 120)
    )
    val stream: Stream[IO, FitMessage] = Stream.emits(messages)

    val summaries = Map(
      Freestyle -> SwimStrokeSummary(3, 75f, 50f, FormattedDuration.pace(120f)),
      Breaststroke -> SwimStrokeSummary(3, 75f, 75f, FormattedDuration.pace(160f))
    )
    val expected =
      SwimReport(
        poolLength = 25f,
        distance = 150f,
        lengthCount = 6,
        duration = FormattedDuration(210f),
        startTime = startTime,
        utcOffsetSecs = 0,
        avgPace = FormattedDuration.pace(140f),
        avgHr = 120,
        rest = FormattedDuration(100f),
        summary = summaries
      )
    processor.process(stream).asserting(_.value shouldEqual expected)
  }

  it should "produce swim report from activity with mixed lap" in withProcessor { processor =>
    val startTime = Instant.now()
    val messages: List[FitMessage] = List(
      LengthMessage(Freestyle, 25f, 0),
      LengthMessage(Freestyle, 35f, 1),
      LengthMessage(Breaststroke, 30f, 2),
      LengthMessage(Breaststroke, 50f, 3),
      LengthMessage(Breaststroke, 40f, 4),
      LengthMessage(Other, 10f, 5, active = false),
      LengthMessage(Freestyle, 30f, 6),
      LapMessage(Mixed, 125f, 5, 0, 180f, 120),
      LapMessage(Mixed, 0f, 0, 5, 100f, 110),
      LapMessage(Freestyle, 25f, 1, 6, 30f, 120),
      SessionMessage(Sport.Swimming, 25f, 150f, 210f, startTime, 120)
    )
    val stream: Stream[IO, FitMessage] = Stream.emits(messages)

    val summaries = Map(
      Freestyle -> SwimStrokeSummary(3, 75f, 50f, FormattedDuration.pace(120f)),
      Breaststroke -> SwimStrokeSummary(3, 75f, 75f, FormattedDuration.pace(160f))
    )
    val expected =
      SwimReport(
        poolLength = 25f,
        distance = 150f,
        lengthCount = 6,
        duration = FormattedDuration(210f),
        startTime = startTime,
        utcOffsetSecs = 0,
        avgPace = FormattedDuration.pace(140f),
        avgHr = 120,
        rest = FormattedDuration(100f),
        summary = summaries
      )
    processor.process(stream).asserting(_.value shouldEqual expected)
  }

  it should "skip non-active length messages" in withProcessor { processor =>
    val startTime = Instant.now()
    val messages: List[FitMessage] = List(
      LengthMessage(Freestyle, 20f, 0),
      LengthMessage(Mixed, 10f, 1, active = false),
      LengthMessage(Breaststroke, 40f, 2),
      LengthMessage(Mixed, 33f, 3, active = false),
      LapMessage(Mixed, 50f, 2, 0, 60f, 110),
      SessionMessage(Sport.Swimming, 25f, 50f, 60f, startTime, 110)
    )
    val stream: Stream[IO, FitMessage] = Stream.emits(messages)

    val summaries = Map(
      Freestyle -> SwimStrokeSummary(1, 25f, 25f, FormattedDuration.pace(80f)),
      Breaststroke -> SwimStrokeSummary(1, 25f, 25f, FormattedDuration.pace(160f))
    )
    val expected =
      SwimReport(
        poolLength = 25f,
        distance = 50f,
        lengthCount = 2,
        duration = FormattedDuration(60f),
        startTime = startTime,
        utcOffsetSecs = 0,
        avgPace = FormattedDuration.pace(120f),
        avgHr = 110,
        rest = FormattedDuration(0f),
        summary = summaries
      )
    processor.process(stream).asserting(_.value shouldEqual expected)
  }

  it should "compute time offset from activity message timestamps" in withProcessor { processor =>
    val startTime = Instant.now()
    val startTimestamp = startTime.getEpochSecond
    val timeOffsetSec = 2.hours.toSeconds
    val messages: List[FitMessage] = List(
      LengthMessage(Freestyle, 20f, 0),
      LengthMessage(Breaststroke, 40f, 1),
      LapMessage(Mixed, 50f, 2, 0, 60f, 110),
      SessionMessage(Sport.Swimming, 25f, 50f, 60f, startTime, 110),
      ActivityMessage(startTimestamp, startTimestamp + timeOffsetSec)
    )
    val stream: Stream[IO, FitMessage] = Stream.emits(messages)

    val summaries = Map(
      Freestyle -> SwimStrokeSummary(1, 25f, 25f, FormattedDuration.pace(80f)),
      Breaststroke -> SwimStrokeSummary(1, 25f, 25f, FormattedDuration.pace(160f))
    )
    val expected =
      SwimReport(
        poolLength = 25f,
        distance = 50f,
        lengthCount = 2,
        duration = FormattedDuration(60f),
        startTime = startTime,
        utcOffsetSecs = timeOffsetSec.toInt,
        avgPace = FormattedDuration.pace(120f),
        avgHr = 110,
        rest = FormattedDuration(0f),
        summary = summaries
      )
    processor.process(stream).asserting(_.value shouldEqual expected)
  }

  private def withProcessor(f: ReportProcessor[IO] => IO[Assertion]): IO[Assertion] = {
    val reader = ReportProcessor.make[IO]
    reader.flatMap(f)
  }
}
