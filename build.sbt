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

organization in Global := "com.codecommit"

publishGithubUser in Global := "djspiewak"
publishFullName in Global := "Daniel Spiewak"

baseVersion in Global := "0.8"

bintrayVcsUrl in Global := Some("git@github.com:djspiewak/sbt-spiewak.git")

sbtPlugin in Global := true
sbtVersion in Global := "1.2.6"

lazy val root = project
  .aggregate(core, bintray, sonatype)
  .in(file("."))
  .settings(name := "root")
  .settings(noPublishSettings)

lazy val core = project
  .in(file("core"))
  .settings(name := "sbt-spiewak")
  .settings(
    addSbtPlugin("io.get-coursier"   % "sbt-coursier"    % "1.1.0-M7"),
    addSbtPlugin("com.dwijnand"      % "sbt-travisci"    % "1.1.3"),
    addSbtPlugin("com.typesafe.sbt"  % "sbt-git"         % "1.0.0"),
    addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.0.0"),
    addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.3.0"),
    addSbtPlugin("io.crashbox"       % "sbt-gpg"         % "0.2.0"))

lazy val bintray = project
  .in(file("bintray"))
  .dependsOn(core)
  .settings(name := "sbt-spiewak-bintray")
  .settings(
    addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4"))

lazy val sonatype = project
  .in(file("sonatype"))
  .dependsOn(core)
  .settings(name := "sbt-spiewak-sonatype")
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3"))
