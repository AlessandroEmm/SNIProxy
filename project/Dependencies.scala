import sbt._

object Version {
  val scala     = "2.11.4"
  val akka      = "2.3.4"
  val spray     = "1.3.2"
  val sprayJson = "1.3.1"
}

object Library {
  val akkaActor      = "com.typesafe.akka" %% "akka-actor"      % Version.akka
  val akkaSlf4j      = "com.typesafe.akka" %% "akka-slf4j"      % Version.akka
  val akkaCluster    = "com.typesafe.akka" %% "akka-cluster"    % Version.akka
  val akkaTestkit    = "com.typesafe.akka" %% "akka-testkit"    % Version.akka
  val sprayRouting   = "io.spray"          %%  "spray-routing"   % Version.spray
  val sprayCan       = "io.spray"          %%  "spray-can"       % Version.spray
  val sprayJson      = "io.spray"          %% "spray-json"      % Version.sprayJson
}

object Dependencies {

  import Library._

  val demoAkka = List(
    akkaActor,
    akkaSlf4j,
    akkaCluster,
    sprayRouting,
    sprayCan,
    sprayJson
  )
}
