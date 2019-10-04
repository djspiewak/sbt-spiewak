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

Global / organization := "com.codecommit"

Global / publishGithubUser := "djspiewak"
Global / publishFullName := "Daniel Spiewak"

Global / baseVersion := "0.10"

Global / bintrayVcsUrl := Some("git@github.com:djspiewak/sbt-spiewak.git")

Global / sbtPlugin := true
Global / sbtVersion := "1.3.2"

lazy val root = project
  .aggregate(core, bintray, sonatype)
  .in(file("."))
  .settings(name := "root")
  .settings(noPublishSettings)

lazy val core = project
  .in(file("core"))
  .settings(name := "sbt-spiewak")
  .settings(
    addSbtPlugin("com.dwijnand"      % "sbt-travisci"    % "1.2.0"),
    addSbtPlugin("com.typesafe.sbt"  % "sbt-git"         % "1.0.0"),
    addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.2.0"),
    addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.6.1"),
    addSbtPlugin("io.crashbox"       % "sbt-gpg"         % "0.2.0"))

lazy val bintray = project
  .in(file("bintray"))
  .dependsOn(core)
  .settings(name := "sbt-spiewak-bintray")
  .settings(
    addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.5"))

lazy val sonatype = project
  .in(file("sonatype"))
  .dependsOn(core)
  .settings(name := "sbt-spiewak-sonatype")
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.6"))
