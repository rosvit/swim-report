package com.rosvit.swimreport

import cats.data.Validated
import cats.implicits.*
import com.monovore.decline.*

import java.nio.file.Path
import scala.util.control.NoStackTrace

enum OutputType {
  case Text, Json
}

final case class ReportArgs(output: Option[OutputType], ignoreIntegrity: Boolean, filePath: Path)

final case class ReportException(msg: String) extends RuntimeException(msg) with NoStackTrace

object Cli {

  private val outputOpts: Opts[Option[OutputType]] =
    Opts
      .option[String](
        long = "output",
        help = "The format of the output. Can be either 'text' or 'json'. Default is text.",
        short = "o",
        metavar = "text|json"
      )
      .map(_.trim.toLowerCase)
      .mapValidated {
        case "text" => Validated.valid(OutputType.Text)
        case "json" => Validated.valid(OutputType.Json)
        case other  => Validated.invalidNel(s"Invalid output type: $other")
      }
      .orNone

  private val ignoreIntegrityFlag: Opts[Boolean] =
    Opts.flag(long = "ignore-integrity", help = "Ignores failed FIT file integrity check.").orFalse

  private val pathOpts: Opts[Path] = Opts.argument[Path](metavar = "FIT file path")

  val reportOpts: Opts[ReportArgs] = (outputOpts, ignoreIntegrityFlag, pathOpts).mapN(ReportArgs.apply)
}
