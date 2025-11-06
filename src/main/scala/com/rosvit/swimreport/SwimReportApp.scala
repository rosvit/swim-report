package com.rosvit.swimreport

import cats.effect.*
import cats.effect.std.Console
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import com.rosvit.swimreport.Cli.*
import com.rosvit.swimreport.OutputType.Text
import com.rosvit.swimreport.buildinfo.BuildInfo
import com.rosvit.swimreport.fit.FitFileStatus.*
import com.rosvit.swimreport.fit.FitReader

import java.nio.file.Path

object SwimReportApp
    extends CommandIOApp(
      name = "swim-report",
      header = "Swimming activity report from FIT file",
      version = BuildInfo.version
    ) {

  private inline val FitSportErrorMessage = "FIT file doesn't contain valid pool swim activity"

  override def main: Opts[IO[ExitCode]] =
    reportOpts
      .map { case ReportArgs(output, ignoreIntegrity, path) =>
        doReport(path, ignoreIntegrity, output.getOrElse(Text))
      }

  private def doReport(fitPath: Path, ignoreIntegrity: Boolean, output: OutputType): IO[ExitCode] =
    (for {
      fitReader <- FitReader.make[IO]
      reportProcessor <- ReportProcessor.make[IO]
      fileStatus <- fitReader.checkIntegrity(fitPath)
      _ <-
        if (fileStatus == Ok || (fileStatus == IntegrityCheckFailed && ignoreIntegrity)) IO.unit
        else IO.raiseError(ReportException(fileStatus.message))
      messages = fitReader.messages(fitPath)
      maybeReport <- reportProcessor.process(messages)
      report <- maybeReport.fold(IO.raiseError(ReportException(FitSportErrorMessage)))(r => IO.pure(r))
      _ <- printReport(output, report)
    } yield ExitCode.Success)
      .recoverWith(cause => Console[IO].errorln(s"ERROR: ${cause.getMessage}").as(ExitCode.Error))

  private def printReport(output: OutputType, report: SwimReport): IO[Unit] = IO.println {
    output match {
      case OutputType.Text => ReportTemplate.text(report)
      case OutputType.Json => ReportTemplate.jsonString(report)
    }
  }
}
