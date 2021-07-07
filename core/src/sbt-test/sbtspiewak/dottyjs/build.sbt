ThisBuild / crossScalaVersions := Seq("2.13.6", "3.0.0")

ThisBuild / baseVersion := "0.1"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

lazy val check = taskKey[Unit]("")

val setting = {
  check := {
    val suffix = if (isDotty.value) "3" else "2"
    List((Compile / unmanagedSourceDirectories).value, (Test / unmanagedSourceDirectories).value) foreach { dirs =>
      if (!dirs.exists(_.getAbsolutePath().endsWith(s"scala-$suffix"))) {
        sys.error(s"$dirs did not contain a scala-$suffix directory")
      }
    }
  }
}

lazy val root = project.in(file(".")).aggregate(core.js, core.jvm).settings(setting)
lazy val core = crossProject(JSPlatform, JVMPlatform).in(file("core"))
  .settings(setting)
