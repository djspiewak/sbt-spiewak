# Versioning

This plugin facilitates a versioning scheme which is based on semver, but with deviations to allow for hash versioning and to avoid pre-selecting compatibility versions when publishing snapshots.

## Compatibility Version

Every commit has a `baseVersion`, declared in the build. This version should be the major/minor components of a semver-compatible version. This version is used to check compatibility with [MiMa](https://github.com/lightbend/migration-manager) as well as dictate base versions on hash snapshots (see below).

As an example, if a commit has `baseVersion := "1.2"`, this is asserting that the contents are *semver compatible* with all versions `1.2.x`. So in a sense, any commit with `baseVersion := "1.2"` may be released as a build version in the `1.2.x` line (e.g. `1.2.8`, `1.2.0`, etc). The correlary of this is that, when changes are made which break compatibility, they should be paired with an appropriate change to the `baseVersion`. The `ci` task exposed by this plugin will guarantee this, assuming it is used by the underlying continuous integration system.

The purpose of this system is to avoid pre-committing to a new version when making a previous release. Compatibility version increments are tied to the changes which break compatibility, which avoids situations like Scala 2.8 (which should have been Scala 3.0) and similar.

The *downside* to this system is what happens when the `baseVersion` is incremented to a new major/minor line due to a compatibility change... and then a subsequent compatibility-breaking change is made before a stable version is published. The most appropriate thing to do under those circumstances would be to increment the `baseVersion` *again*, but this will result in skipping a version in the final published line. This situation also happens quite frequently during active development, so it's not at all a rare hypothetical.

In general, this situation is handled by ignoring the mutual-compatibility constraint within `baseVersion` lines when no stable versions within that line have been published. This is still semver-compliant, though it does make hash snapshot eviction a bit less reliable. Since hash snapshots don't evict without warnings, there should be little pain caused by this.

## Hash Snapshots

This plugin does everything in its power to eliminate the scourge of `-SNAPSHOT` versions. It also assumes you use Git, like most people. All git commits are considered "releaseable" by the plugin (via the `release` task). If a given commit does not have an associated version tag (see below), then the commit will be versioned according to the `baseVersion` + a truncated git hash. For example: `1.2-abc123e`. This version is guaranteed to have compatibility with all stable versions in the `1.2.x` line at the time of publication.

There are several advantages to this scheme. First, hash snapshots are stable and thus usable in reproducible builds (which is to say, usable). Second, it gives downstream users the ability to create and depend on their own published versions.

Let's imagine that some downstream user needs a particular bugfix of library `foo` which is at version `1.2.3`. Let's further imagine that the bugfix is binary-compatible. They make the change and submit a pull request (like a good citizen of open source) but they need to use that change in their own downstream dependency *now*, without waiting for the upstream maintainer to merge their PR and create a new stable release. Hash snapshots provide a very elegant solution to this problem: the downstream user can just create a `release` from the commit which represents the HEAD of their fix branch (e.g. `1.2-fe992a7`).

This is even much safer than private Nexus override tricks since hash snapshots with identical versions are guaranteed to *be* identical, which is not a property which is achievable through any other snapshot versioning scheme.
