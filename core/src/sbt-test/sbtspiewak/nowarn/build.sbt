ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.12"," 2.13.3", "0.27.0-RC1", "3.0.0-M1")

ThisBuild / baseVersion := "0.1"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

scalacOptions ++= {
  if (isDotty.value) Seq.empty // @nowarn compiles, but doesn't work
  else Seq("-Xfatal-warnings")
}

enablePlugins(NowarnCompatPlugin)
