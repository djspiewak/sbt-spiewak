ThisBuild / crossScalaVersions := Seq("2.12.16", "2.13.6")

ThisBuild / baseVersion := "0.1"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

lazy val root = project.in(file(".")).aggregate(`with`, without)

lazy val `with` = project.in(file("with"))
lazy val without = project.in(file("without"))
  .settings(
    crossScalaVersions := Seq("2.13.6"),

    Compile / compile := {
      val old = (Compile / compile).value

      if (scalaVersion.value != "2.13.6")
        sys.error("wrong!")
      else
        old
    })
