# sbt-spiewak [![Build Status](https://travis-ci.org/djspiewak/sbt-spiewak.svg?branch=master)](https://travis-ci.org/djspiewak/sbt-spiewak)

This plugin basically just exists to allow me to more conveniently setup my baseline SBT configuration, which has evolved somewhat over the years, and is also becoming quite unwieldy when solely represented in [giter8 format](https://github.com/djspiewak/base.g8). **You probably shouldn't use this plugin.** If you do though, let me know how it works out!

## Usage

Put this in your `plugins.sbt`:

```sbt
resolvers += Resolver.url("djspiewak-sbt-plugins", url("https://dl.bintray.com/djspiewak/sbt-plugins"))(Resolver.ivyStylePatterns)

// for stock functionality (no publication defaults)
addSbtPlugin("com.codecommit" % "sbt-spiewak" % "0.3.3")

// publishing to bintray
addSbtPlugin("com.codecommit" % "sbt-spiewak-bintray" % "0.3.3")

// publishing to sonatype
addSbtPlugin("com.codecommit" % "sbt-spiewak-sonatype" % "0.3.3")
```

Then, in your `build.sbt`, make sure you set a value for `baseVersion`:

```sbt
baseVersion := "0.1"
```

Or something like that.

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
  + sbt-pgp
    * Fixes most of the glitches and bugs related to sbt-pgp's setting scoping and default configuration
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
- Hard-coded defaults which assume `com.codecommit` and `Daniel Spiewak` in all the places
  + ...this is totally a feature!

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

Everything is basically hard-coded to assume you are me. If you are not me, then you should override the following keys:

```sbt
organization := "com.my.groupId"
organizationName := "John Smith"

developers := Developer("johnsmith", "John Smith", "@johnsmith", url("https://github.com/johnsmith"))

// if using sonatype...
sonatypeProfileName := "com.my.groupId"
```

You may also wish to override `licenses` and/or `startYear` if you aren't using Apache 2.0 and/or the year is not 2018.
