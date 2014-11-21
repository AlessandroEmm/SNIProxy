organization := "alessandro meyer"

name := "SNI Reverse Proxy"

version := "0.1.0"

scalaVersion := "2.11.4"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.2.0"

resolvers ++= List(
  "spray-releases" at "http://repo.spray.io",
  "spray-nightlies" at "http://nightlies.spray.io"
)

libraryDependencies ++= Dependencies.demoAkka

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8"
)
