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
      LengthMessage(Freestyle, 25f),
      LengthMessage(Freestyle, 35f),
      LengthMessage(Breaststroke, 30f),
      LengthMessage(Breaststroke, 50f),
      LengthMessage(Breaststroke, 40f),
      LengthMessage(Freestyle, 30f),
      LapMessage(Freestyle, 50f, 2, 60f, 120),
      LapMessage(Breaststroke, 75f, 3, 120f, 120),
      LapMessage(Freestyle, 25f, 1, 60f, 120),
      LapMessage(Mixed, 0f, 0, 100f, 110),
      SessionMessage(Sport.Swimming, 25f, 150f, 240f, startTime, 120)
    )
    val stream: Stream[IO, FitMessage] = Stream.emits(messages)

    val summaries = Map(
      Freestyle -> SwimStrokeSummary(3, 75f, 50f, FormattedDuration.pace(120f)),
      Breaststroke -> SwimStrokeSummary(3, 75f, 75f, FormattedDuration.pace(160f))
    )
    val expected =
      SwimReport(25f, 150f, 6, FormattedDuration(240f), startTime, 0, 120, FormattedDuration(100f), summaries)
    processor.process(stream).asserting(_.value shouldEqual expected)
  }

  it should "produce swim report from activity with mixed lap" in withProcessor { processor =>
    val startTime = Instant.now()
    val messages: List[FitMessage] = List(
      LengthMessage(Freestyle, 25f),
      LengthMessage(Freestyle, 35f),
      LengthMessage(Breaststroke, 30f),
      LengthMessage(Breaststroke, 50f),
      LengthMessage(Breaststroke, 40f),
      LengthMessage(Freestyle, 30f),
      LapMessage(Mixed, 100f, 4, 180f, 120),
      LapMessage(Freestyle, 50f, 2, 60f, 120),
      LapMessage(Mixed, 0f, 0, 100f, 110),
      SessionMessage(Sport.Swimming, 25f, 150f, 240f, startTime, 120)
    )
    val stream: Stream[IO, FitMessage] = Stream.emits(messages)

    val summaries = Map(
      Freestyle -> SwimStrokeSummary(3, 75f, 50f, FormattedDuration.pace(120f)),
      Breaststroke -> SwimStrokeSummary(3, 75f, 25f, FormattedDuration.pace(160f))
    )
    val expected =
      SwimReport(25f, 150f, 6, FormattedDuration(240f), startTime, 0, 120, FormattedDuration(100f), summaries)
    processor.process(stream).asserting(_.value shouldEqual expected)
  }

  it should "skip non-active length messages" in withProcessor { processor =>
    val startTime = Instant.now()
    val messages: List[FitMessage] = List(
      LengthMessage(Freestyle, 20f),
      LengthMessage(Mixed, 10f, active = false),
      LengthMessage(Breaststroke, 40f),
      LengthMessage(Mixed, 33f, active = false),
      LapMessage(Mixed, 50f, 2, 60f, 110),
      SessionMessage(Sport.Swimming, 25f, 50f, 60f, startTime, 110)
    )
    val stream: Stream[IO, FitMessage] = Stream.emits(messages)

    val summaries = Map(
      Freestyle -> SwimStrokeSummary(1, 25f, 25f, FormattedDuration.pace(80f)),
      Breaststroke -> SwimStrokeSummary(1, 25f, 25f, FormattedDuration.pace(160f))
    )
    val expected =
      SwimReport(25f, 50f, 2, FormattedDuration(60f), startTime, 0, 110, FormattedDuration(0f), summaries)
    processor.process(stream).asserting(_.value shouldEqual expected)
  }

  it should "compute time offset from activity message timestamps" in withProcessor { processor =>
    val startTime = Instant.now()
    val startTimestamp = startTime.getEpochSecond
    val timeOffsetSec = 2.hours.toSeconds
    val messages: List[FitMessage] = List(
      LengthMessage(Freestyle, 20f),
      LengthMessage(Breaststroke, 40f),
      LapMessage(Mixed, 50f, 2, 60f, 110),
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
