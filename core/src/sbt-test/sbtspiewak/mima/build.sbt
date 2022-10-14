ThisBuild / crossScalaVersions := Seq("2.12.17", "2.13.6")

ThisBuild / baseVersion := "0.2"

ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

lazy val setupEarly = taskKey[Unit]("")
lazy val checkSemVerEarly = taskKey[Unit]("")
lazy val checkScalaVerEarly = taskKey[Unit]("")

lazy val setupLate = taskKey[Unit]("")
lazy val checkSemVerLate = taskKey[Unit]("")
lazy val checkScalaVerLate = taskKey[Unit]("")

versionIntroduced += "2.13" -> "0.2.1"

setupEarly := {
  import scala.sys.process._

  "git init".!
  "git add .".!
  "git commit -m init".!

  "git tag v0.1.0".!
  "git tag v0.1.1".!
  "git tag v0.2.0".!
  "git tag v0.2.1".!
  "git tag v0.2.2".!

  "touch blah".!
  "git add blah".!
  "git commit -m blah".!
}

checkSemVerEarly := {
  val prev = mimaPreviousArtifacts.value

  val expectedVersions = if (scalaVersion.value == "2.12.17")
    Set("0.2.0", "0.2.1", "0.2.2")
  else
    Set("0.2.1", "0.2.2")

  val prefix = organization.value % s"${name.value}_${CrossVersion.binaryScalaVersion(scalaVersion.value)}"
  val expected = expectedVersions.map(prefix % _)

  if (prev != expected) {
    sys.error(s"expected $expected; got $prev")
  }
}

checkScalaVerEarly := {
  val prev = mimaPreviousArtifacts.value

  val expectedVersions = if (scalaVersion.value == "2.12.17")
    Set("0.2.0", "0.2.1", "0.2.2")
  else
    Set("0.2.1", "0.2.2")

  val prefix = organization.value % s"${name.value}_${CrossVersion.binaryScalaVersion(scalaVersion.value)}"
  val expected = expectedVersions.map(prefix % _)

  if (prev != expected) {
    sys.error(s"expected $expected; got $prev")
  }
}

setupLate := {
  import scala.sys.process._

  "git tag v1.1.0".!
  "git tag v1.1.1".!
  "git tag v1.2.0".!
  "git tag v1.2.1".!
  "git tag v1.2.2".!

  "touch blah2".!
  "git add blah2".!
  "git commit -m blah2".!
}

checkSemVerLate := {
  val prev = mimaPreviousArtifacts.value

  val expectedVersions = Set("1.1.0", "1.1.1", "1.2.0", "1.2.1", "1.2.2")
  val prefix = organization.value % s"${name.value}_${CrossVersion.binaryScalaVersion(scalaVersion.value)}"
  val expected = expectedVersions.map(prefix % _)

  if (prev != expected) {
    sys.error(s"expected $expected; got $prev")
  }
}

checkScalaVerLate := {
  val prev = mimaPreviousArtifacts.value

  val expectedVersions = Set("1.2.0", "1.2.1", "1.2.2")
  val prefix = organization.value % s"${name.value}_${CrossVersion.binaryScalaVersion(scalaVersion.value)}"
  val expected = expectedVersions.map(prefix % _)

  if (prev != expected) {
    sys.error(s"expected $expected; got $prev")
  }
}
