import sbt._

object Version {
  val scala     = "2.10.2"
  val akka      = "2.2.1"
  val logback   = "1.0.13"
  val scalaTest = "2.0.RC1-SNAP4"
}

object Library {
  val akkaActor      = "com.typesafe.akka" %% "akka-actor"      % Version.akka
  val akkaSlf4j      = "com.typesafe.akka" %% "akka-slf4j"      % Version.akka
  val akkaTestkit    = "com.typesafe.akka" %% "akka-testkit"    % Version.akka
  val logbackClassic = "ch.qos.logback"    %  "logback-classic" % Version.logback
  val scalaTest      = "org.scalatest"     %% "scalatest"       % Version.scalaTest
}

object Dependencies {

  import Library._

  val demoAkka = List(
    scalaTest % "test"
  )
}
