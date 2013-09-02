import sbt._

object Version {
  val scala     = "2.10.2"
  val akka      = "2.2.1"
  val spray     = "1.2-20130822"
  val sprayJson = "1.2.5"
  val logback   = "1.0.13"
  val scalaTest = "2.0.RC1-SNAP4"
}

object Library {
  val akkaActor      = "com.typesafe.akka" %% "akka-actor"      % Version.akka
  val akkaSlf4j      = "com.typesafe.akka" %% "akka-slf4j"      % Version.akka
  val akkaCluster    = "com.typesafe.akka" %% "akka-cluster"    % Version.akka
  val akkaTestkit    = "com.typesafe.akka" %% "akka-testkit"    % Version.akka
  val sprayRouting   = "io.spray"          %  "spray-routing"   % Version.spray
  val sprayCan       = "io.spray"          %  "spray-can"       % Version.spray
  val sprayJson      = "io.spray"          %% "spray-json"      % Version.sprayJson
  val logbackClassic = "ch.qos.logback"    %  "logback-classic" % Version.logback
  val scalaTest      = "org.scalatest"     %% "scalatest"       % Version.scalaTest
}

object Dependencies {

  import Library._

  val demoAkka = List(
    akkaActor,
    akkaSlf4j,
    akkaCluster,
    sprayRouting,
    sprayCan,
    sprayJson,
    logbackClassic,
    scalaTest   % "test",
    akkaTestkit % "test"
  )
}
