import Dependencies.*

ThisBuild / scalaVersion := "3.3.4"
ThisBuild / version := "0.1"
ThisBuild / organization := "com.rosvit"
ThisBuild / organizationName := "RoSvit"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "swim-report",
    Compile / run / fork := true,
    assembly / mainClass := Some("com.rosvit.swimreport.SwimReportApp"),
    scalacOptions ++= Seq("-no-indent", "-rewrite"),
    buildInfoPackage := "com.rosvit.swimreport.buildinfo",
    libraryDependencies ++= Seq(
      cats,
      catsEffect,
      declineEffect,
      fs2Core,
      fitSdk,
      scalaTest,
      catsEffectScalaTest
    )
  )
