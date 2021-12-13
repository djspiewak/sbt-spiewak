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

import xerial.sbt.Sonatype, Sonatype.autoImport._

object SpiewakSonatypePlugin extends AutoPlugin {

  override def requires = SpiewakPlugin && Sonatype

  override def trigger = allRequirements

  override def buildSettings =
    addCommandAlias("release", "; reload; project /; +mimaReportBinaryIssuesIfRelevant; +publishIfRelevant; sonatypeBundleReleaseIfRelevant")

  override def projectSettings = Seq(
    publishMavenStyle := true,    // we want to do this unconditionally, even if publishing a plugin
    sonatypeProfileName := organization.value,
    publishTo := sonatypePublishToBundle.value,
    commands += sonatypeBundleReleaseIfRelevant
  )

  private def sonatypeBundleReleaseIfRelevant: Command =
    Command.command("sonatypeBundleReleaseIfRelevant") { state1 =>
      val isSnap = state1.getSetting(isSnapshot).getOrElse(false)
      if (!isSnap)
        Command.process("sonatypeBundleRelease", state1)
      else {
        // Check for a published hash version.
        val ver = state1.setting(ThisBuild / version)
        val nonSnap = ver.replace("-SNAPSHOT", "")
        val base = state1.setting(ThisBuild / baseDirectory)
        val hashDirectory = base / "target" / "sonatype-staging" / nonSnap
        if (hashDirectory.exists()) {
          // A hash release exists. Release it.
          val state2 = state1.appendWithSession(Seq((ThisBuild / version) := nonSnap))
          val state3 = Command.process("sonatypeBundleRelease", state2)
          state3.appendWithSession(Seq((ThisBuild / version) := ver))
        } else {
          // Do nothing.
          state1
        }
      }
    }
}
