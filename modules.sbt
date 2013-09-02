// This is needed to add the multi-jvm configuration
lazy val demoAkka = Project(
  "demo-akka",
  file("."),
  configurations = Configurations.default :+ com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
)
