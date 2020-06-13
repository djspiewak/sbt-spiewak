ThisBuild / crossScalaVersions := Seq("2.13.2", "0.24.0-RC1")

ThisBuild / baseVersion := "0.1"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

lazy val root = project.in(file(".")).aggregate(core.js, core.jvm)
lazy val core = crossProject(JSPlatform, JVMPlatform).in(file("core"))
  .settings(dottyJsSettings(ThisBuild / crossScalaVersions))
