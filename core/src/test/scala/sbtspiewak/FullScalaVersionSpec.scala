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

import org.specs2.mutable.Specification

class FullScalaVersionSpec extends Specification {

  "full scala version parsing" should {
    "handle scala 2.13.3" in {
      val input = "2.13.3"
      input must beLike {
        case FullScalaVersion(2, 13, 3, MRC.Final, None) => ok
      }
    }

    "handle scala 2.13.0-M1" in {
      val input = "2.13.0-M1"
      input must beLike {
        case FullScalaVersion(2, 13, 0, MRC.Milestone(1), None) => ok
      }
    }

    "handle scala 3 rc snapshots" in {
      val input = "3.0.0-RC1-bin-SNAPSHOT"
      input must beLike {
        case FullScalaVersion(3, 0, 0, MRC.ReleaseCandidate(1), Some("-bin-SNAPSHOT")) => ok
      }
    }
  }
}
