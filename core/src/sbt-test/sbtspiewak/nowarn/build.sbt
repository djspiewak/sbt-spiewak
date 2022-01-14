ThisBuild / crossScalaVersions := Seq("2.11.12", "2.13.8", "2.13.6", "3.0.0")

ThisBuild / baseVersion := "0.1"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

scalacOptions ++= {
  if (isDotty.value) Seq.empty // @nowarn compiles, but doesn't work
  else Seq("-Xfatal-warnings")
}

enablePlugins(NowarnCompatPlugin)
