package com.rosvit.swimreport.fit

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rosvit.swimreport.fit.FitFileStatus.{NotExist, NotFitFile, Ok}
import com.rosvit.swimreport.fit.FitReaderSpec.*
import com.rosvit.swimreport.fit.Sport.Swimming
import com.rosvit.swimreport.fit.SwimStroke.*
import org.scalatest.Assertion
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.{Path, Paths}
import java.time.Instant

class FitReaderSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  "checkIntegrity" should "fail if file does not exists" in withReader { reader =>
    val fitPath = Paths.get("/tmp/not_existing.fit")
    reader.checkIntegrity(fitPath).asserting(_ shouldBe NotExist)
  }

  it should "fail if file is not FIT file" in withReader { reader =>
    reader.checkIntegrity(getResourcePath("/notfit.txt")).asserting(_ shouldBe NotFitFile)
  }

  it should "successfully pass on valid FIT file" in withReader { reader =>
    reader.checkIntegrity(fitFilePath).asserting(_ shouldBe Ok)
  }

  "messages" should "read FIT file and return stream of messages" in withReader { reader =>
    reader.messages(fitFilePath).compile.toList.asserting { messages =>
      messages.collect { case m: SessionMessage => m } should have size 1
      messages.collect { case m: LapMessage => m } should have size 42
      messages.collect { case m: LengthMessage => m } should have size 92

      messages should contain allOf (
        LengthMessage(Breaststroke, 30.43f, 1),
        LapMessage(Freestyle, 50f, 2, 78, 73.87f, 119),
        SessionMessage(Swimming, 25f, 2300f, 4011.63f, Instant.parse("2024-09-27T16:09:40Z"), 119)
      )
    }
  }

  private def withReader(f: FitReader[IO] => IO[Assertion]): IO[Assertion] = {
    val reader = FitReader.make[IO]
    reader.flatMap(f)
  }
}

object FitReaderSpec {

  val fitFilePath: Path = getResourcePath("/data.fit")

  def getResourcePath(resource: String): Path =
    Paths.get(getClass.getResource(resource).toURI)
}
