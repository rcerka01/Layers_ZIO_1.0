ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "Layers"
  )

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.4-2"
)