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

object FullScalaVersion {
  private val Release = """^(\d+)\.(\d+)\.(\d+)$""".r
  private val Milestone = """^(\d+)\.(\d+)\.(\d+)-M(\d+)$""".r
  private val ReleaseCandidate = """^(\d+)\.(\d+)\.(\d+)-RC(\d+)$""".r
  private val Snapshot = """^(\d+)\.(\d+)\.(\d+)-M(\d+)(.+)$""".r
  private val Nightly = """^(\d+)\.(\d+)\.(\d+)-(bin|pre)-([0-9a-f]{7})$""".r

  /**
   * Returns major, minor, build, milestone/rc (optional), qualifier (optional)
   */
  def unapply(version: String): Option[(Int, Int, Int, MRC, Option[String])] = version match {
    case Release(major, minor, build) =>
      Some((major.toInt, minor.toInt, build.toInt, MRC.Final, None))

    case Milestone(major, minor, build, milestone) =>
      Some((major.toInt, minor.toInt, build.toInt, MRC.Milestone(milestone.toInt), None))

    case ReleaseCandidate(major, minor, build, rc) =>
      Some((major.toInt, minor.toInt, build.toInt, MRC.ReleaseCandidate(rc.toInt), None))

    case Snapshot(major, minor, build, milestone, qualifier) =>
      Some((major.toInt, minor.toInt, build.toInt, MRC.Milestone(milestone.toInt), Some(qualifier)))

    case Nightly(major, minor, build, binPre, hash) =>
      Some((major.toInt, minor.toInt, build.toInt, MRC.Nightly(binPre == "bin", hash), None))
  }
}
