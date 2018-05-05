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
import coursier.Keys._
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import com.typesafe.tools.mima.plugin.MimaPlugin, MimaPlugin.autoImport._
import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.pgp.PgpKeys._
import sbttravisci.TravisCiPlugin, TravisCiPlugin.autoImport._

import scala.sys.process._

object SpiewakPlugin extends AutoPlugin {

  override def requires =
    GitPlugin &&
    SbtPgp &&
    TravisCiPlugin &&
    MimaPlugin &&
    coursier.CoursierPlugin &&
    plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    // strictly x.y.z
    val ReleaseTag = """^v((?:\d+\.){2}\d+)$""".r

    /*
     * Compatibility version.  Use this to declare what version with
     * which `master` remains in compatibility.  This is literally
     * backwards from how -SNAPSHOT versioning works, but it avoids
     * the need to pre-declare (before work is done) what kind of
     * compatibility properties the next version will have (i.e. major
     * or minor bump).
     *
     * As an example, the builds of a project might go something like
     * this:
     *
     * - 0.1-hash1
     * - 0.1-hash2
     * - 0.1-hash3
     * - 0.1
     * - 0.1-hash1
     * - 0.2-hash2
     * - 0.2
     * - 0.2-hash1
     * - 0.2-hash2
     * - 1.0-hash3
     * - 1.0-hash4
     * - 1.0
     *
     * The value of BaseVersion starts at 0.1 and remains there until
     * compatibility with the 0.1 line is lost, which happens just
     * prior to the release of 0.2.  Then the base version again remains
     * 0.2-compatible until that compatibility is broken, with the major
     * version bump of 1.0.  Again, this is all to avoid pre-committing
     * to a major/minor bump before the work is done (see: Scala 2.8).
     */
    lazy val baseVersion = git.baseVersion

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
    addCommandAlias("ci", "; clean; test; mimaReportBinaryIssues") ++
    {
      // this needs to be here because sbt-pgp is written incorrectly and uses inScope(Global) in buildSettings
      import com.typesafe.sbt.pgp._

      inScope(Global)(Seq(
        pgpSecretRing := pgpPublicRing.value,   // workaround for sbt/sbt-pgp#126
        useGpg := true,

        // workaround for other madness in sbt-pgp
        pgpSigner := new CommandLineGpgSigner(gpgCommand.value, useGpgAgent.value, pgpSecretRing.value.getPath, pgpSigningKey.value, pgpPassphrase.value),
        pgpVerifierFactory := new BouncyCastlePgpVerifierFactory(pgpCmdContext.value)))
    } ++
    Seq(
      organization := "com.codecommit",
      organizationName := "Daniel Spiewak",

      startYear := Some(2018),

      licenses += (("Apache-2.0", url("http://www.apache.org/licenses/"))),

      coursierUseSbtCredentials := true,
      coursierChecksums := Nil,      // workaround for nexus sync bugs

      isSnapshot := version.value endsWith "SNAPSHOT",

      pomIncludeRepository := { _ => false },

      developers += Developer("djspiewak", "Daniel Spiewak", "@djspiewak", url("http://www.codecommit.com")),

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
      git.gitUncommittedChanges := "git status -s".!!.trim.length > 0)

  override def projectSettings = AutomateHeaderPlugin.projectSettings ++ Seq(
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary),

    // Adapted from Rob Norris' post at https://tpolecat.github.io/2014/04/11/scalac-flags.html
    scalacOptions ++= Seq(
      "-language:_",
      "-deprecation",
      "-encoding", "UTF-8", // yes, this is 2 args
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code"),

    scalacOptions ++= {
      if (isTravisBuild.value)
        Seq("-Xfatal-warnings")
      else
        Seq.empty
    },

    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, major)) if major >= 11 => Seq(
          "-Ywarn-unused-import", // Not available in 2.10
          "-Ywarn-numeric-widen" // In 2.10 this produces a some strange spurious error
        )
        case _ => Seq.empty
      }
    },

    scalacOptions ++= {
      val Scala11Version = """^2\.11\.(\d+)$""".r

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) =>
          Seq("-Ypartial-unification")

        case Some((2, 11)) =>
          val Scala11Version(build) = scalaVersion.value

          if (build.toInt >= 11)
            Seq("-Ypartial-unification")
          else
            Seq.empty

        // note that -Ypartial-unification is defaulted on 2.13
        case _ => Seq.empty
      }
    },

    scalacOptions ++= {
      val Scala12Version = """^2\.12\.(\d+)$""".r
      val Scala13MVersion = """^2\.13\.(\d+)-M(\d+).*""".r

      val numCPUs = java.lang.Runtime.getRuntime.availableProcessors()
      val settings = Seq(s"-Ybackend-parallelism", numCPUs.toString)

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) =>
          val Scala12Version(build) = scalaVersion.value

          if (build.toInt >= 5)
            settings
          else
            Seq.empty

        case Some((2, 13)) =>
          scalaVersion.value match {
            case Scala13MVersion(_, milestone) =>
              // it was introduced in 2.13.0-M4
              if (milestone.toInt >= 4)
                settings
              else
                Seq.empty

            // anything in 2.13 which is NOT a milestone will have the setting
            case _ =>
              settings
          }

        case Some((2, major)) if major > 13 =>
          settings

        // anything prior to 2.12 will lack the setting
        case _ =>
          Seq.empty
      }
    },

    scalacOptions in Test += "-Yrangepos",

    scalacOptions in (Compile, console) ~= (_ filterNot (Set("-Xfatal-warnings", "-Ywarn-unused-import").contains)),

    scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,

    libraryDependencies ++= {
      scalaVersion.value match {
        case "2.11.8" => Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))
        case "2.10.6" => Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))
        case _ => Seq.empty
      }
    },

    mimaPreviousArtifacts := {
      val current = version.value
      val org = organization.value
      val n = name.value

      val TagBase = """^(\d+)\.(\d+).*"""r
      val TagBase(major, minor) = baseVersion.value

      if (sbtPlugin.value) {
        Set.empty
      } else {
        val tags = "git tag --list".!!.split("\n").map(_.trim)

        val versions =
          tags.filter(_.startsWith(s"v$major.$minor")).map(_.substring(1))

        versions.filterNot(current ==).map(v => org %% n % v).toSet
      }
    })
}
