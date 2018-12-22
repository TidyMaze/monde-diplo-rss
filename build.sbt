name := """monde-diplo-rss"""

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
  "com.markatta" %% "scalenium" % "1.0.3",
  "com.rometools" % "rome" % "1.11.1",
  "com.github.cb372" %% "scalacache-core" % "0.27.0",
  "com.github.cb372" %% "scalacache-guava" % "0.27.0"
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ )
