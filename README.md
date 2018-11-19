# sbt-spiewak [![Build Status](https://travis-ci.org/djspiewak/sbt-spiewak.svg?branch=master)](https://travis-ci.org/djspiewak/sbt-spiewak) [![Download](https://api.bintray.com/packages/djspiewak/sbt-plugins/sbt-spiewak/images/download.svg)](https://bintray.com/djspiewak/sbt-plugins/sbt-spiewak/_latestVersion)

This plugin basically just exists to allow me to more conveniently setup my baseline SBT configuration, which has evolved somewhat over the years, and is also becoming quite unwieldy when solely represented in [giter8 format](https://github.com/djspiewak/base.g8).

If you generally agree with my opinions on how projects should be set up, though, then this is probably a really excellent plugin to base on! Between this plugin and my giter8 template, you can get a new Scala project up and running and publishing to Bintray or Sonatype within about five minutes. As an example, check out this quick screencast:

<iframe id="ytplayer" type="text/html" width="640" height="360"
  src="https://www.youtube.com/embed/SjcMKHpY1WU?autoplay=0&origin=https://github.com/djspiewak/sbt-spiewak"
  frameborder="0"/>

TLDR, it's really really easy.

## Usage

Put one of the following into your `plugins.sbt`:

```sbt
// for stock functionality (no publication defaults)
addSbtPlugin("com.codecommit" % "sbt-spiewak" % "<version>")

// publishing to bintray
addSbtPlugin("com.codecommit" % "sbt-spiewak-bintray" % "<version>")

// publishing to sonatype
addSbtPlugin("com.codecommit" % "sbt-spiewak-sonatype" % "<version>")
```

Then, in your `build.sbt`, make sure you set a value for `baseVersion`, `organization`, `publishGithubUser` and `publishFullName`:

```sbt
organization in ThisBuild := "com.codecommit"

baseVersion in ThisBuild := "0.1"

publishGithubUser in ThisBuild := "djspiewak"
publishFullName in ThisBuild := "Daniel Spiewak"
```

Or something like that.

If you have a multi-module build and need a subproject to *not* publish (as is commonly done with the `root` project), bring in `noPublishSettings`. For example:

```sbt
lazy val root = project
  .aggregate(core, bintray, sonatype)
  .in(file("."))
  .settings(name := "root")
  .settings(noPublishSettings)
```

## Features

- Baseline plugin setup
  + coursier
  + sbt-travisci
  + sbt-git
    * With sane git versioning settings
    * Also with fixed `git-status` stuff
  + sbt-header
    * Assumes Apache 2.0 license
  + sbt-bintray (or sonatype!)
    * With fixed support for Travis builds
  + sbt-sonatype (or bintray!)
    * With fixed snapshot publication URLs
  + sbt-gpg
  + sbt-mima
    * Infers previous versions by using git tags
    * Automatically runs on `ci` and `release`
- Sane scalac settings
  + Including `-Ybackend-parallelism` where supported
- SI-2712 fix across scala versions (dating back to 2.10)
- kind-projector
- `release` and `ci` command aliases
  + Ensures bintray package existence
  + Performs sonatype release steps
  + Assumes your root project is declared as `lazy val root = project`, etc

### Bintray Requirements

You will need to additionally define the following setting:

```sbt
bintrayVcsUrl in Global := Some("git@github.com:djspiewak/sbt-spiewak.git")
```

### Sonatype Requirements

You will additionally need to define the following settings:

```sbt
homepage in Global := Some(url("https://github.com/djspiewak/sbt-spiewak")),

scmInfo in Global := Some(ScmInfo(url("https://github.com/djspiewak/sbt-spiewak"),
  "git@github.com:djspiewak/sbt-spiewak.git")))
```

## Defaults Which You May Wish to Override...

You may consider overriding any of the following keys, which are hard-coded to defaults that I believe are sane:

- `licenses` (defaults to Apache 2.0)
- `developers` (defaults to just yourself, using the `publishFullName` and `publishGithubUser`)
- `startYear` (defaults to 2018)
- `strictSemVer` (defaults to `true`)
  + When set to `true`, it disallows breaking binary compatibility in any release which does not increment the *major* component unless the major component is `0` (i.e. semantic versioning). Many Scala projects break binary compatibility in *minor* releases, such as Scala itself. This scheme is sometimes referred to as "scala ver". Setting `strictSemVer in ThisBuild := false` will relax the MiMa compatibility checks and allow you to perform such breakage if desired.
