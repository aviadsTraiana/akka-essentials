name := "akka-essentials"

version := "0.1"

scalaVersion := "2.13.3"

val akkaVersion = "2.6.8"
val scalaTestVersion = "3.2.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)