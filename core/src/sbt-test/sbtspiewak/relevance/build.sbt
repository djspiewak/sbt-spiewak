ThisBuild / scalaVersion := crossScalaVersions.value.last
ThisBuild / crossScalaVersions := Seq("2.12.11", "2.13.2")

ThisBuild / baseVersion := "0.1"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

lazy val root = project.in(file(".")).aggregate(`with`, without)

lazy val `with` = project.in(file("with"))
lazy val without = project.in(file("without"))
  .settings(
    crossScalaVersions := Seq("2.13.2"),

    Compile / compile := {
      val old = (Compile / compile).value

      if (scalaVersion.value != "2.13.2")
        sys.error("wrong!")
      else
        old
    })
