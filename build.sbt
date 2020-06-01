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

Global / baseVersion := "0.14"

Global / bintrayVcsUrl := Some("git@github.com:djspiewak/sbt-spiewak.git")

Global / sbtPlugin := true
Global / sbtVersion := "1.3.11"

scalaVersion := crossScalaVersions.value.last
crossScalaVersions := Seq("2.12.11")

lazy val root = project
  .aggregate(core, bintray, sonatype)
  .in(file("."))
  .settings(name := "root")
  .settings(noPublishSettings)

lazy val core = project
  .in(file("core"))
  .settings(name := "sbt-spiewak")
  .settings(
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
