import sbt.*

object Dependencies {

  val catsVersion = "2.13.0"
  val catsEffectVersion = "3.6.3"
  val declineVersion = "2.6.0"
  val fs2Version = "3.12.2"
  val circeVersion = "0.14.15"
  val fitSdkVersion = "21.188.0"
  val scalaTestVersion = "3.2.19"
  val catsEffectScalaTestVersion = "1.7.0"

  lazy val cats = "org.typelevel" %% "cats-core" % catsVersion
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  lazy val declineEffect = "com.monovore" %% "decline-effect" % declineVersion
  lazy val fs2Core = "co.fs2" %% "fs2-core" % fs2Version
  lazy val circe = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion

  lazy val fitSdk = "com.garmin" % "fit" % fitSdkVersion

  lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  lazy val catsEffectScalaTest = "org.typelevel" %% "cats-effect-testing-scalatest" % catsEffectScalaTestVersion % Test
}
