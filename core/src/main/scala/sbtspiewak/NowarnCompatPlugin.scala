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

import sbt._
import sbt.Keys._

import explicitdeps.ExplicitDepsPlugin, ExplicitDepsPlugin.autoImport._

object NowarnCompatPlugin extends AutoPlugin {
  object autoImport {
    val nowarnCompatSilencerVersion = settingKey[String]("Version of the silencer compiler plugin")
    val nowarnCompatAnnotationProvider = settingKey[Option[ModuleID]]("Module providing scala.annotation.nowarn ")
  }

  import autoImport._

  override def trigger = noTrigger
  override def requires = ExplicitDepsPlugin

  override def globalSettings = Seq(
    nowarnCompatSilencerVersion := "1.7.1",
    nowarnCompatAnnotationProvider := Some("org.scala-lang.modules" %% "scala-collection-compat" % "2.3.0"),
  )

  override def projectSettings = Seq(
    libraryDependencies ++= {
      scalaVersion.value match {
        case FullScalaVersion(3, _, _, _, _) => Seq.empty // New Dotties
        case FullScalaVersion(0, _, _, _, _) => Seq.empty // Old Dotties
        case FullScalaVersion(2, 13, x, _, _) if x >= 2 => Seq.empty // Native support
        case FullScalaVersion(2, 13, x, _, _) =>
          sys.error(s"NowarnCompatPlugin: Unsupported Scala version: ${scalaVersion.value}. Scala 2.13 must be at least 2.13.2.")
        case _ =>
          Seq(
            compilerPlugin(("com.github.ghik" % "silencer-plugin" % nowarnCompatSilencerVersion.value).cross(CrossVersion.full)),
            ("com.github.ghik" % "silencer-lib" % nowarnCompatSilencerVersion.value % Provided).cross(CrossVersion.full),
            ("com.github.ghik" % "silencer-lib" % nowarnCompatSilencerVersion.value % Test).cross(CrossVersion.full)
          ) ++
          (nowarnCompatAnnotationProvider.value match {
            case Some(moduleId) =>
              sLog.value.info(s"NowarnCompatPlugin: Scala ${scalaVersion.value} doesn't support @nowarn. Adding ${moduleId}.")
              Seq(moduleId)
            case None =>
              sLog.value.warn(s"NowarnCompatPlugin: Scala ${scalaVersion.value} doesn't support @nowarn. Project needs to supply its own @nowarn implementation, or set `nowarnCompatAnnotationProvider`.")
              Seq.empty
          })
      }
    },
    unusedCompileDependenciesFilter -= moduleFilter("com.github.ghik", "silencer-lib"),
  )
}
