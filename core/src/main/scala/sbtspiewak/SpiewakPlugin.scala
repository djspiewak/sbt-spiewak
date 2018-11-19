/*
 * Copyright 2018 Daniel Spiewak
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
import coursier.Keys._
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import _root_.io.crashbox.gpg.SbtGpg
import sbttravisci.TravisCiPlugin, TravisCiPlugin.autoImport._

import scala.sys.process._
import scala.util.Try

object SpiewakPlugin extends AutoPlugin {

  override def requires =
    GitPlugin &&
    SbtGpg &&
    TravisCiPlugin &&
    MimaPlugin &&
    coursier.CoursierPlugin &&
    plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    // strictly x.y.z
    val ReleaseTag = """^v((?:\d+\.){2}\d+)$""".r

    /**
     * https://github.com/djspiewak/sbt-spiewak/versioning/blob/589b9ea/versioning.md
     */
    lazy val baseVersion = git.baseVersion

    lazy val publishGithubUser = settingKey[String]("The github username of the main developer")
    lazy val publishFullName = settingKey[String]("The full name of the main developer")

    def noPublishSettings = Seq(
      publish := {},
      publishLocal := {},
      publishArtifact := false,

      mimaPreviousArtifacts := Set.empty,
      skip in publish := true)
  }

  import autoImport._

  private val DeprecatedReleaseTag = """^v((?:\d+\.)?\d+)$""".r

  override def buildSettings =
    GitPlugin.autoImport.versionWithGit ++
    addCommandAlias("ci", "; project root; clean; test; mimaReportBinaryIssues") ++
    Seq(
      organizationName := publishFullName.value,

      startYear := Some(2018),

      licenses += (("Apache-2.0", url("http://www.apache.org/licenses/"))),

      coursierUseSbtCredentials := true,
      coursierChecksums := Nil,      // workaround for nexus sync bugs

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

      git.formattedShaVersion := {
        val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)

        git.gitHeadCommit.value map { _.substring(0, 7) } map { sha =>
          git.baseVersion.value + "-" + sha + suffix
        }
      },

      // jgit does weird things...
      git.gitUncommittedChanges := Try("git status -s".!!.trim.length > 0).getOrElse(true))

  override def projectSettings = AutomateHeaderPlugin.projectSettings ++ Seq(
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.7" cross CrossVersion.binary),

    // Adapted from Rob Norris' post at https://tpolecat.github.io/2014/04/11/scalac-flags.html
    scalacOptions ++= Seq(
      "-language:_",
      "-deprecation",
      "-encoding", "UTF-8", // yes, this is 2 args
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code"),

    scalacOptions ++= {
      scalaVersion.value match {
        case FullScalaVersion(2, minor, _, _, _) if minor < 13 =>
          Seq("-Yno-adapted-args")
        case _ =>
          Seq.empty
      }
    },

    scalacOptions ++= {
      if (isTravisBuild.value)
        Seq("-Xfatal-warnings")
      else
        Seq.empty
    },

    scalacOptions ++= {
      val warnings211 = Seq(
        "-Ywarn-unused-import", // Not available in 2.10
        "-Ywarn-numeric-widen") // In 2.10 this produces a some strange spurious error

      val warnings212 = Seq("-Xlint:-unused,_")

      scalaVersion.value match {
        case FullScalaVersion(2, minor, _, _, _) if minor >= 12 =>
          warnings211 ++ warnings212

        case FullScalaVersion(2, minor, _, _, _) if minor >= 11 =>
          warnings211

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
      val settings = Seq(s"-Ybackend-parallelism", numCPUs.toString)

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

    scalacOptions in Test += "-Yrangepos",

    scalacOptions in (Compile, console) --= Seq("-Xlint", "-Ywarn-unused-import"),
    scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,

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
      val n = name.value

      val TagBase = """^(\d+)\.(\d+).*"""r
      val TagBase(major, minor) = baseVersion.value

      val isPre = major == 0

      if (sbtPlugin.value) {
        Set.empty
      } else {
        val tags = Try("git tag --list".!!.split("\n").map(_.trim)).getOrElse(new Array[String](0))

        // in semver, we allow breakage in minor releases if major is 0, otherwise not
        val prefix = if (isPre)
          s"v$major.$minor"
        else
          s"v$major"

        val versions =
          tags.filter(_.startsWith(prefix)).map(_.substring(1))

        versions.filterNot(current ==).map(v => org %% n % v).toSet
      }
    })
}
