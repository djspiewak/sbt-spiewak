ThisBuild / crossScalaVersions := Seq("2.12.16", "2.13.6")

ThisBuild / baseVersion := "0.1"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

lazy val check = taskKey[Unit]("check the things")

check := {
  if (scalaVersion.value != "2.13.6") {
    sys.error(s"expected '2.13.6' got '${scalaVersion.value}'")
  }
}
