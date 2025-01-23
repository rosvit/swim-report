import Dependencies.*
import org.typelevel.scalacoptions.ScalacOptions

ThisBuild / scalaVersion := "3.3.4"
ThisBuild / version := "0.2.0"
ThisBuild / organization := "com.rosvit"
ThisBuild / organizationName := "RoSvit"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "swim-report",
    Compile / run / fork := true,
    Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
    assembly / mainClass := Some("com.rosvit.swimreport.SwimReportApp"),
    assembly / assemblyJarName := "swim-report.jar",
    scalacOptions ++= Seq("-no-indent", "-rewrite"),
    buildInfoPackage := "com.rosvit.swimreport.buildinfo",
    libraryDependencies ++= Seq(
      cats,
      catsEffect,
      declineEffect,
      fs2Core,
      circe,
      circeGeneric,
      fitSdk,
      scalaTest,
      catsEffectScalaTest
    )
  )
