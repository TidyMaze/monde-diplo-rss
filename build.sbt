name := """play-getting-started"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers ++= Seq(
  // required for the scalaz-streams dependency from specs2 :(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  ws,
  "com.markatta" %% "scalenium" % "1.0.3"
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ )
