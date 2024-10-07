package com.rosvit.swimreport

import cats.effect.{ExitCode, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import com.rosvit.swimreport.Cli.reportOpts
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class CliSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {
  import CliSpec.*

  "Application" should "accept fit file path as argument" in {
    TestApp.run(List(TestFitFile)).asserting(_ shouldBe ExitCode.Success)
  }

  it should "accept --ignore-integrity" in {
    TestApp.run(List("--ignore-integrity", TestFitFile)).asserting(_ shouldBe ExitCode.Success)
  }

  it should "accept json output format" in {
    TestApp.run(List("--output", "json", TestFitFile)).asserting(_ shouldBe ExitCode.Success)
  }

  it should "accept text output format" in {
    TestApp.run(List("--output", "text", TestFitFile)).asserting(_ shouldBe ExitCode.Success)
  }

  it should "accept output format as short option" in {
    TestApp.run(List("-o", "text", TestFitFile)).asserting(_ shouldBe ExitCode.Success)
  }

  it should "fail on unsupported output format" in {
    TestApp.run(List("-o", "other", TestFitFile)).asserting(_ shouldBe ExitCode.Error)
  }
}

object CliSpec {
  inline val TestFitFile = "~/Documents/test.fit"

  object TestApp extends CommandIOApp("test", "test", true, "1.0") {
    override def main: Opts[IO[ExitCode]] = reportOpts.map(_ => IO(ExitCode.Success))
  }
}
