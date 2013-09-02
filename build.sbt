organization := "name.heikoseeberger"

name := "demo-akka"

version := "0.1.0"

scalaVersion := "2.10.2"

libraryDependencies ++= Dependencies.demoAkka

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8"
)

// initialCommands in console := "import name.heikoseeberger.demoakka._"
