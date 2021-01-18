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

import sbtghactions.GenerativePlugin.autoImport._

object SonatypeCiReleasePlugin extends AutoPlugin {

  object autoImport {
    lazy val spiewakCiReleaseSnapshots = settingKey[Boolean]("Controls whether or not snapshots should be released on main (default: false)")
    lazy val spiewakMainBranches = settingKey[Seq[String]]("The primary branch(es) for your repository (default: [])")
  }

  import autoImport._

  override def requires = SpiewakPlugin

  override def trigger = noTrigger

  override def globalSettings = Seq(
    spiewakCiReleaseSnapshots := false,
    spiewakMainBranches := Seq())

  override def buildSettings = Seq(
    githubWorkflowEnv ++= Map(
      "SONATYPE_USERNAME" -> s"$${{ secrets.SONATYPE_USERNAME }}",
      "SONATYPE_PASSWORD" -> s"$${{ secrets.SONATYPE_PASSWORD }}",
      "PGP_SECRET" -> s"$${{ secrets.PGP_SECRET }}"),

    githubWorkflowPublishTargetBranches := {
      val seed = if (spiewakCiReleaseSnapshots.value)
        spiewakMainBranches.value.map(b => RefPredicate.Equals(Ref.Branch(b)))
      else
        Seq.empty

      RefPredicate.StartsWith(Ref.Tag("v")) +: seed
    },

    githubWorkflowTargetTags += "v*",

  githubWorkflowPublishPreamble +=
    WorkflowStep.Run(
      List("echo $PGP_SECRET | base64 -d | gpg --import"),
      name = Some("Import signing key")
    ),

  githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("release"))))
}
