ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.12", "2.13.1", "2.13.2", "3.0.0-M1")

ThisBuild / baseVersion := "0.1"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

scalacOptions += "-Xfatal-warnings"

enablePlugins(NowarnCompatPlugin)
