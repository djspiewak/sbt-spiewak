/*
 * Copyright 2018-2021 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbtspiewak

import sbt._, Keys._

import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.git
import com.typesafe.tools.mima.plugin.MimaPlugin, MimaPlugin.autoImport._

import de.heikoseeberger.sbtheader.{AutomateHeaderPlugin, HeaderPlugin, License, SpdxLicense}, HeaderPlugin.autoImport._

import explicitdeps.ExplicitDepsPlugin.autoImport._

import _root_.io.crashbox.gpg.SbtGpg

import sbtcrossproject.CrossPlugin, CrossPlugin.autoImport.crossProjectPlatform

import sbtghactions.{GenerativeKeys, GenerativePlugin, GitHubActionsKeys, GitHubActionsPlugin, WorkflowStep}, GenerativeKeys._, GitHubActionsKeys._

import scala.sys.process._
import scala.util.Try

object SpiewakPlugin extends AutoPlugin {

  override def requires =
    GitPlugin &&
    SbtGpg &&
    GitHubActionsPlugin &&
    GenerativePlugin &&
    MimaPlugin &&
    CrossPlugin &&
    plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {

    val ReleaseTag = """^v((?:\d+\.){2}\d+(?:-.*)?)$""".r

    /**
     * https://github.com/djspiewak/sbt-spiewak/versioning/blob/589b9ea/versioning.md
     */
    lazy val baseVersion = git.baseVersion

    lazy val strictSemVer = settingKey[Boolean]("Set to true to forbid breaking changes in the minor releases (strict semantic versioning; default: true)")
    lazy val fatalWarningsInCI = settingKey[Boolean]("Convert compiler warnings into errors under CI builds (default: true)")
    lazy val versionIntroduced = settingKey[Map[String, String]]("A map from *binary* scalaVersion -> version (e.g. '2.13') used to indicate that a particular crossScalaVersions value was introduced in a given version (default: empty)")

    lazy val publishGithubUser = settingKey[String]("The github username of the main developer")
    lazy val publishFullName = settingKey[String]("The full name of the main developer")

    lazy val undeclaredCompileDependenciesTestIfRelevant = taskKey[Unit]("A wrapper around the `undeclaredCompileDependenciesTest` task which checks to ensure the current scalaVersion is in crossScalaVersions and the platform is the JVM")
    lazy val unusedCompileDependenciesTestIfRelevant = taskKey[Unit]("A wrapper around the `unusedCompileDependenciesTest` task which checks to ensure the current scalaVersion is in crossScalaVersions and the platform is the JVM")
    lazy val testIfRelevant = taskKey[Unit]("A wrapper around the `test` task which checks to ensure the current scalaVersion is in crossScalaVersions")
    lazy val mimaReportBinaryIssuesIfRelevant = taskKey[Unit]("A wrapper around the `test` task which checks to ensure the current scalaVersion is in crossScalaVersions")

    lazy val publishSnapshotsAsHashReleases = settingKey[Boolean]("Overrides the snapshot releases and switches to stable hash releases instead")
    lazy val publishIfRelevant = taskKey[Unit]("A wrapper around the `publish` task which checks to ensure the current scalaVersion is in crossScalaVersions")
    lazy val publishLocalIfRelevant = taskKey[Unit]("A wrapper around the `publishLocal` task which checks to ensure the current scalaVersion is in crossScalaVersions")

    lazy val endYear = settingKey[Option[Int]]("A dual to startYear: the year in which the project ended (usually current year). If this is set, then licenses will be encoded as a range from startYear to endYear. Otherwise, only startYear will apply. (default: None)")
    lazy val isDotty = settingKey[Boolean]("True if building with Scala 3")

    @deprecated("Use .enablePlugin(NoPublishPlugin)", "0.18.0")
    val noPublishSettings = Seq(
      publish := {},
      publishLocal := {},
      publishArtifact := false,

      mimaPreviousArtifacts := Set.empty,
      skip in publish := true)

    def filterTaskWhereRelevant(delegate: TaskKey[Unit]) =
      Def.taskDyn {
        val cross = crossScalaVersions.value
        val ver = scalaVersion.value

        if (cross.contains(ver))
          Def.task(delegate.value)
        else
          Def.task(streams.value.log.warn(s"skipping `${delegate.key.label}` in ${name.value}: $ver is not in $cross"))
      }

    // why isn't this in sbt itself?
    def replaceCommandAlias(name: String, contents: String): Seq[Setting[State => State]] =
      Seq(GlobalScope / onLoad ~= { (f: State => State) =>
        f andThen { s: State =>
          BasicCommands.addAlias(BasicCommands.removeAlias(s, name), name, contents)
        }
      })
  }

  import autoImport._

  private val DeprecatedReleaseTag = """^v((?:\d+\.)?\d+)$""".r
  private val Description = """^.*-(\d+)-[a-zA-Z0-9]+$""".r

  private val spdxMapping =
    License.asInstanceOf[{ val spdxLicenses: Vector[SpdxLicense] }].spdxLicenses.foldLeft(Map[String, SpdxLicense]()) { (acc, lic) =>
      acc + (lic.spdxIdentifier -> lic)
    }

  override def globalSettings = Seq(
    endYear := None,
    fatalWarningsInCI := true,
    versionIntroduced := Map(),
    crossScalaVersions := Seq("2.13.2"),
    Def.derive(scalaVersion := crossScalaVersions.value.last, default = true),
    Def.derive(isDotty := scalaVersion.value.startsWith("3.")))

  override def buildSettings =
    GitPlugin.autoImport.versionWithGit ++
    addCommandAlias("ci", List(
      "project /",
      "headerCheckAll",
      "clean",
      // "unusedCompileDependenciesTestIfRelevant",
      "testIfRelevant",
      "mimaReportBinaryIssuesIfRelevant"
    ).mkString("; ", "; ", "")) ++
    addCommandAlias("releaseLocal", "; reload; project /; +publishLocalIfRelevant") ++
    Seq(
      organizationName := publishFullName.value,

      strictSemVer := true,

      // both of these are enforced by the automatic mima magic
      versionScheme := {
        if (strictSemVer.value)
          Some("early-semver")
        else
          Some("pvp")
      },

      startYear := Some(2021),

      licenses += (("Apache-2.0", url("http://www.apache.org/licenses/"))),

      // disable automatic generation of the publication workflow
      githubWorkflowPublishTargetBranches := Seq(),
      githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("ci"))),

      isSnapshot := version.value endsWith "SNAPSHOT",

      pomIncludeRepository := { _ => false },

      developers += Developer(
        publishGithubUser.value,
        publishFullName.value,
        s"@${publishGithubUser.value}",
        url(s"https://github.com/${publishGithubUser.value}")),

      git.gitTagToVersionNumber := {
        val log = sLog.value

        {
          case ReleaseTag(version) =>
            Some(version)

          case DeprecatedReleaseTag(version) =>
            log.warn(s"ignoring non-semver-compliant version: $version")
            None

          case _ =>
            None
        }
      },

      publishSnapshotsAsHashReleases := false,
      git.formattedShaVersion := {
        val hashOverride = publishSnapshotsAsHashReleases.value
        val condition =
          if (hashOverride) git.gitUncommittedChanges.value // old sbt-spiewak behavior
          else git.gitCurrentTags.value.isEmpty || git.gitUncommittedChanges.value // default sbt-git behavior

        val suffix = git.makeUncommittedSignifierSuffix(condition, git.uncommittedSignifier.value)

        val description = Try("git describe --tags --match v*".!!.trim).toOption
        val optDistance = description collect {
          case Description(distance) => distance + "-"
        }

        val distance = optDistance.getOrElse("")

        git.gitHeadCommit.value map { _.substring(0, 7) } map { sha =>
          git.baseVersion.value + "-" + distance + sha + suffix
        }
      },

      git.gitUncommittedChanges := Try("git status -s".!!.trim.length > 0).getOrElse(true),

      git.gitHeadCommit := Try("git rev-parse HEAD".!!.trim).toOption,
      git.gitCurrentTags := Try("git tag --contains HEAD".!!.trim.split("\\s+").toList.filter(_ != "")).toOption.toList.flatten,

      commands += publishHashIfRelevant
    )

  override def projectSettings =
    AutomateHeaderPlugin.projectSettings ++
    Seq(
      headerLicense := {
        val range = (startYear.value, endYear.value) match {
          case (Some(start), Some(end)) =>
            Some(s"$start-$end")

          case (None, Some(year)) =>
            Some(year.toString)

          case (Some(year), None) =>
            Some(year.toString)

          case (None, None) =>
            None
        }

        // the following was copied from sbt-header
        val licenseName = licenses.value match {
          case (name, _) :: Nil => Some(name)
          case _ => None
        }

        for {
          name <- licenseName
          license <- spdxMapping.get(name)
          year <- range
        } yield license(year, organizationName.value, headerLicenseStyle.value)
      },

      undeclaredCompileDependenciesTestIfRelevant := filterTaskWhereRelevant(undeclaredCompileDependenciesTest).value,
      unusedCompileDependenciesTestIfRelevant := filterTaskWhereRelevant(unusedCompileDependenciesTest).value,
      Test / testIfRelevant := filterTaskWhereRelevant(Test / test).value,
      mimaReportBinaryIssuesIfRelevant := filterTaskWhereRelevant(mimaReportBinaryIssues).value,
      publishIfRelevant := filterTaskWhereRelevant(publish).value,
      publishLocalIfRelevant := filterTaskWhereRelevant(publishLocal).value,
      SbtGpg.autoImport.gpgWarnOnFailure := isSnapshot.value,

      libraryDependencies ++= {
        if (isDotty.value)
          Nil
        else
          Seq(
            compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
            compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
          )
      },

      // Adapted from Rob Norris' post at https://tpolecat.github.io/2014/04/11/scalac-flags.html
      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8", // yes, this is 2 args
        "-feature",
        "-unchecked"),

      scalacOptions ++= {
        scalaVersion.value match {
          case FullScalaVersion(2, minor, _, _, _) if minor < 13 =>
            Seq("-Yno-adapted-args", "-Ywarn-unused-import")
          case _ =>
            Seq.empty
        }
      },

      scalacOptions ++= {
        if (githubIsWorkflowBuild.value && !isDotty.value && fatalWarningsInCI.value)
          Seq("-Xfatal-warnings")
        else
          Seq.empty
      },

      scalacOptions ++= {
        val warningsNsc = Seq("-Xlint", "-Ywarn-dead-code")

        val warnings211 = Seq(
          "-Ywarn-numeric-widen") // In 2.10 this produces a some strange spurious error

        val warnings212 = Seq("-Xlint:-unused,_")

        val removed213 = Set("-Xlint:-unused,_", "-Xlint")
        val warnings213 = Seq(
          "-Xlint:deprecation",
          "-Wunused:nowarn",
          "-Wdead-code",
          "-Wextra-implicit",
          "-Wnumeric-widen",
          "-Wunused:implicits",
          "-Wunused:explicits",
          "-Wunused:imports",
          "-Wunused:locals",
          "-Wunused:params",
          "-Wunused:patvars",
          "-Wunused:privates",
          "-Wvalue-discard")

        val warningsDotty = Seq()

        scalaVersion.value match {
          case FullScalaVersion(0, minor, _, _, _) if minor >= 24 =>
            warningsDotty

          case FullScalaVersion(3, _, _, _, _) =>
            warningsDotty

          case FullScalaVersion(2, minor, _, _, _) if minor >= 13 =>
            (warnings211 ++ warnings212 ++ warnings213 ++ warningsNsc).filterNot(removed213)

          case FullScalaVersion(2, minor, _, _, _) if minor >= 12 =>
            warnings211 ++ warnings212 ++ warningsNsc

          case FullScalaVersion(2, minor, _, _, _) if minor >= 11 =>
            warnings211 ++ warningsNsc

          case _ => Seq.empty
        }
      },

      scalacOptions ++= {
        scalaVersion.value match {
          case FullScalaVersion(2, 12, _, _, _) =>
            Seq("-Ypartial-unification")

          case FullScalaVersion(2, 11, build, _, _) if build >= 11 =>
            Seq("-Ypartial-unification")

          case FullScalaVersion(2, 13, 0, MRC.Milestone(milestone), qualifier) if milestone < 4 || (milestone == 4 && qualifier.isDefined) =>
            Seq("-Ypartial-unification")

          case _ =>
            Seq.empty
        }
      },

      scalacOptions ++= {
        val numCPUs = java.lang.Runtime.getRuntime.availableProcessors()
        val settings = Seq(s"-Ybackend-parallelism", scala.math.min(16, numCPUs).toString)

        scalaVersion.value match {
          case FullScalaVersion(2, 12, build, _, _) if build >= 5 =>
            settings

          // setting was introduced in 2.13.0-M4 (final)
          case FullScalaVersion(2, 13, 0, MRC.Milestone(milestone), qualifier) if milestone > 4 || (milestone == 4 && !qualifier.isDefined) =>
            settings

          case FullScalaVersion(2, 13, _, _, _) =>
            settings

          case _ =>
            Seq.empty
        }
      },

      scalacOptions ++= {
        if (isDotty.value && crossScalaVersions.value.forall(_.startsWith("3.")))
          Seq("-Ykind-projector:underscores")
        else if (isDotty.value)
          Seq("-language:implicitConversions", "-Ykind-projector", "-source:3.0-migration")
        else
          Seq("-language:_")
      },

      Test / scalacOptions ++= {
        if (isDotty.value)
          Seq()
        else
          Seq("-Yrangepos")
      },

      Compile / console / scalacOptions --= Seq(
        "-Xlint",
        "-Ywarn-unused-import",
        "-Wextra-implicit",
        "-Wunused:implicits",
        "-Wunused:explicits",
        "-Wunused:imports",
        "-Wunused:locals",
        "-Wunused:params",
        "-Wunused:patvars",
        "-Wunused:privates"),

      Test / console / scalacOptions := (Compile / console / scalacOptions).value,

      Compile / doc / scalacOptions ++= {
        if (isDotty.value)
          Seq("-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath)
        else {
          val isSnapshot = git.gitCurrentTags.value.map(git.gitTagToVersionNumber.value).flatten.isEmpty

          val versionOrHash =
            if (!isSnapshot)
              Some(s"v${version.value}")
            else
              git.gitHeadCommit.value

          val infoOpt = scmInfo.value
          versionOrHash.toSeq flatMap { vh =>
            infoOpt.toSeq flatMap { info =>
              val path = s"${info.browseUrl}/blob/$vh€{FILE_PATH}.scala"
               Seq("-doc-source-url", path, "-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath)
            }
          }
        }
      },

      scalacOptions ++= {
        val flag = Some(scalaVersion.value) collect {
          case FullScalaVersion(2, _, _, _, _) =>
            "-P:scalajs:mapSourceURI:"

          case FullScalaVersion(3, 0, _, MRC.Milestone(m), _) if m >= 2 =>
            "-scalajs-mapSourceURI:"

          case FullScalaVersion(3, 0, _, MRC.ReleaseCandidate(_) | MRC.Final, _) =>
            "-scalajs-mapSourceURI:"

          case FullScalaVersion(3, m, _, _, _) if m > 0 =>
            "-scalajs-mapSourceURI:"
        }

        if (crossProjectPlatform.?.value.map(_.identifier == "js").getOrElse(false)) {
          val hasVersion = git.gitCurrentTags.value.map(git.gitTagToVersionNumber.value).flatten.nonEmpty
          val versionOrHash =
            if (hasVersion)
              Some(s"v${version.value}")
            else
              git.gitHeadCommit.value

          val l = (LocalRootProject / baseDirectory).value.toURI.toString

          val infoOpt = scmInfo.value
          versionOrHash flatMap { v =>
            flag flatMap { f =>
              infoOpt map { info =>
                val g = s"${info.browseUrl.toString.replace("github.com", "raw.githubusercontent.com")}/$v/"
                s"${f}${l}->${g}"
              }
            }
          }
        } else {
          None
        }
      },

      javacOptions ++= Seq(
        "-encoding", "utf8",
        "-Xlint:all",
      ),

      javacOptions ++= {
        if (githubIsWorkflowBuild.value && !isDotty.value && fatalWarningsInCI.value)
          Seq("-Werror")
        else
          Seq.empty
      },

      libraryDependencies ++= {
        scalaVersion.value match {
          case FullScalaVersion(2, 10, build, _, _) if build >= 6 =>
            Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))

          case FullScalaVersion(2, 11, 8, _, _) =>
            Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))

          case _ =>
            Seq.empty
        }
      },

      mimaPreviousArtifacts := {
        val current = version.value
        val org = organization.value
        val n = moduleName.value

        val TagBase = """^(\d+)\.(\d+).*"""r
        val TagBase(major, minor) = baseVersion.value

        val isPre = major.toInt == 0

        if (sbtPlugin.value || !crossProjectPlatform.?.value.map(_.identifier == "jvm").getOrElse(true)) {
          Set.empty
        } else {
          val tags = Try("git tag --list".!!.split("\n").map(_.trim)).getOrElse(new Array[String](0))

          // in semver, we allow breakage in minor releases if major is 0, otherwise not
          val Pattern = if (isPre || !strictSemVer.value)
            s"^v($major\\.$minor\\.\\d+)$$".r
          else
            s"^v($major\\.\\d+\\.\\d+)$$".r

          val versions = tags collect {
            case Pattern(version) => version
          }

          val notCurrent = versions.filterNot(current ==)

          val FullVersion = """^(\d+)\.(\d+)\.(\d+)$""".r
          val reduced = versionIntroduced.value.get(CrossVersion.binaryScalaVersion(scalaVersion.value)) match {
            case Some(FullVersion(boundMaj, boundMin, boundRev)) =>
              // we assume you don't want more than 1000 versions per component
              val boundOrd = boundMaj.toInt * 1000 * 1000 + boundMin.toInt * 1000 + boundRev.toInt

              notCurrent filter {
                case FullVersion(maj, min, rev) =>
                  (maj.toInt * 1000 * 1000 + min.toInt * 1000 + rev.toInt) >= boundOrd
              }

            case None =>
              notCurrent
          }

          reduced.map(v => org % s"${n}_${CrossVersion.binaryScalaVersion(scalaVersion.value)}" % v).toSet
        }
      
      },

      pomPostProcess := { node =>
        import scala.xml._
        import scala.xml.transform._
        def stripIf(f: Node => Boolean) =
          new RewriteRule {
            override def transform(n: Node) =
              if (f(n)) NodeSeq.Empty else n
          }
        val stripTestScope = stripIf(n => n.label == "dependency" && (n \ "scope").text == "test")
        new RuleTransformer(stripTestScope).transform(node)(0)
      },

      unusedCompileDependenciesFilter -=
        moduleFilter("org.scala-js", "scalajs-library*") |
        moduleFilter("org.scala-lang", "scala3-library*"))

  private def publishHashIfRelevant: Command =
    Command.command("publishHashIfRelevant") { state1 =>
      val cross = state1.setting(crossScalaVersions)
      val ver = state1.setting(scalaVersion)

      if (cross.contains(ver)) {
        val old = state1.setting(publishSnapshotsAsHashReleases)
        val state2 = state1.appendWithSession(Seq(ThisBuild / publishSnapshotsAsHashReleases := true))
        val state3 = Command.process("publishIfRelevant", state2)
        state3.appendWithSession(Seq(ThisBuild / publishSnapshotsAsHashReleases := old))
      } else {
        state1
      }
    }
}
