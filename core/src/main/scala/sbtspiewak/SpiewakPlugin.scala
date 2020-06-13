/*
 * Copyright 2020 Daniel Spiewak
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

import de.heikoseeberger.sbtheader.AutomateHeaderPlugin

import dotty.tools.sbtplugin.DottyPlugin, DottyPlugin.autoImport._

import _root_.io.crashbox.gpg.SbtGpg

import sbtcrossproject.CrossPlugin, CrossPlugin.autoImport.crossProjectPlatform

import sbtghactions.{GenerativeKeys, GenerativePlugin, GitHubActionsKeys, GitHubActionsPlugin}, GenerativeKeys._, GitHubActionsKeys._

import scala.sys.process._
import scala.util.Try

object SpiewakPlugin extends AutoPlugin {

  override def requires =
    GitPlugin &&
    SbtGpg &&
    GitHubActionsPlugin &&
    GenerativePlugin &&
    MimaPlugin &&
    DottyPlugin &&
    CrossPlugin &&
    plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    // strictly x.y.z
    val ReleaseTag = """^v((?:\d+\.){2}\d+)$""".r

    /**
     * https://github.com/djspiewak/sbt-spiewak/versioning/blob/589b9ea/versioning.md
     */
    lazy val baseVersion = git.baseVersion

    lazy val strictSemVer = settingKey[Boolean]("Set to true to forbid breaking changes in the minor releases (strict semantic versioning)")

    lazy val publishGithubUser = settingKey[String]("The github username of the main developer")
    lazy val publishFullName = settingKey[String]("The full name of the main developer")

    lazy val testIfRelevant = taskKey[Unit]("A wrapper around the `test` task which checks to ensure the current scalaVersion is in crossScalaVersions")

    val noPublishSettings = Seq(
      publish := {},
      publishLocal := {},
      publishArtifact := false,

      mimaPreviousArtifacts := Set.empty,
      skip in publish := true)

    val dottyLibrarySettings = Seq(
      libraryDependencies :=
        libraryDependencies.value.map(_.withDottyCompat(scalaVersion.value)))

    val dottyJsSettings = Seq(
      crossScalaVersions := {
        val old = crossScalaVersions.value
        if (isDotty.value && crossProjectPlatform.value.identifier != "jvm")
          old.filter(_.startsWith("2."))
        else
          old
      })

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

  override def globalSettings = Seq(
    crossScalaVersions := Seq("2.13.2"),
    Def.derive(scalaVersion := crossScalaVersions.value.last))

  override def buildSettings =
    GitPlugin.autoImport.versionWithGit ++
    addCommandAlias("ci", "; project /; headerCheck; clean; testIfRelevant; mimaReportBinaryIssues") ++
    Seq(
      organizationName := publishFullName.value,

      strictSemVer := true,

      startYear := Some(2020),

      licenses += (("Apache-2.0", url("http://www.apache.org/licenses/"))),

      // disable automatic generation of the publication workflow
      githubWorkflowPublishTargetBranches := Seq(),

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
    Test / testIfRelevant := Def.taskDyn {
      val cross = crossScalaVersions.value
      val ver = scalaVersion.value

      if (cross.contains(ver))
        Def.task((Test / test).value)
      else
        Def.task(streams.value.log.warn(s"skipping `test` in ${name.value}: $ver is not in $cross"))
    }.value,

    libraryDependencies ++= {
      if (isDotty.value)
        Nil
      else
        Seq(compilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full))
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
          Seq("-Yno-adapted-args")
        case _ =>
          Seq.empty
      }
    },

    scalacOptions ++= {
      if (githubIsWorkflowBuild.value && !isDotty.value)
        Seq("-Xfatal-warnings")
      else
        Seq.empty
    },

    scalacOptions ++= {
      val YwarnUnusedImport = "-Ywarn-unused-import"

      val warningsNsc = Seq("-Xlint", "-Ywarn-dead-code")
      val warningsDotty = Seq()

      val warnings211 = Seq(
        YwarnUnusedImport, // Not available in 2.10
        "-Ywarn-numeric-widen") // In 2.10 this produces a some strange spurious error

      val warnings212 = Seq("-Xlint:-unused,_")

      scalaVersion.value match {
        case FullScalaVersion(0, minor, _, _, _) if minor >= 24 =>
          warningsDotty

        case FullScalaVersion(3, _, _, _, _) =>
          warningsDotty

        case FullScalaVersion(2, minor, _, _, _) if minor >= 13 =>
          (warnings211 ++ warnings212 ++ warningsNsc).filterNot(_ == YwarnUnusedImport)    // no idea where this went...

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

    scalacOptions ++= {
      if (isDotty.value)
        Seq("-language:Scala2Compat,implicitConversions", "-Ykind-projector")
      else
        Seq("-language:_")
    },

    Test / scalacOptions ++= {
      if (isDotty.value)
        Seq()
      else
        Seq("-Yrangepos")
    },

    Compile / console / scalacOptions --= Seq("-Xlint", "-Ywarn-unused-import"),
    Test / console / scalacOptions := (scalacOptions in (Compile, console)).value,

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
        val prefix = if (isPre || !strictSemVer.value)
          s"v$major.$minor"
        else
          s"v$major"

        val versions =
          tags.filter(_.startsWith(prefix)).map(_.substring(1))

        versions.filterNot(current ==).map(v => org %% n % v).toSet
      }
    },

    // dottydoc really doesn't work at all right now
    Compile / doc / sources := {
      val old = (Compile / doc / sources).value
      if (isDotty.value)
        Seq()
      else
        old
    })
}
