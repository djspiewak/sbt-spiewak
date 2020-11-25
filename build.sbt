/*
 * Copyright 2019 Daniel Spiewak
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

replaceCommandAlias("ci", "; project /; headerCheck; clean; test; scripted; mimaReportBinaryIssues")

Global / organization := "com.codecommit"

Global / publishGithubUser := "djspiewak"
Global / publishFullName := "Daniel Spiewak"

Global / baseVersion := "0.18"

Global / bintrayVcsUrl := Some("git@github.com:djspiewak/sbt-spiewak.git")

Global / sbtPlugin := true
Global / sbtVersion := "1.4.3"

ThisBuild / crossScalaVersions := Seq("2.12.12")

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Run(
    List(
      "git config --global user.email nobody@github.com",
      "git config --global user.name 'GitHub Actions'"),
    name = Some("Configure git"))

lazy val root = project
  .aggregate(core, bintray, sonatype)
  .in(file("."))
  .settings(name := "root")
  .enablePlugins(NoPublishPlugin)

lazy val core = project
  .in(file("core"))
  .settings(name := "sbt-spiewak")
  .settings(
    libraryDependencies += "org.specs2" %% "specs2-core" % "4.10.5" % Test,

    scriptedLaunchOpts ++= Seq("-Dplugin.version=" + version.value),
    scriptedBufferLog := true)
  .enablePlugins(SbtPlugin)

lazy val bintray = project
  .in(file("bintray"))
  .dependsOn(core)
  .settings(name := "sbt-spiewak-bintray")

lazy val sonatype = project
  .in(file("sonatype"))
  .dependsOn(core)
  .settings(name := "sbt-spiewak-sonatype")
