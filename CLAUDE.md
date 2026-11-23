# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@.claude/rules/javadoc.md

## Commands

```bash
./gradlew build :ctrlprivacy-build:build         # Build and check every module and the plugins
./gradlew spotlessApply                          # Fix formatting (Palantir Java Format)
./gradlew integrationTest                        # Tests tagged "integration" (needs Docker)
./gradlew publishToMavenLocal :ctrlprivacy-build:publishToMavenLocal   # Try a -SNAPSHOT in a service
```

Requires JDK 25. The Gradle wrapper (`./gradlew`) is included, so don't install Gradle separately.

## What this repo is

The shared Java components of Ctrl Privacy's Spring Boot APIs, modeled on ReMo's
`remo-components` (`~/projects/ai.remo.app/module/components`). Every module is
`org.ctrlprivacy:ctrlprivacy-<name>`, every plugin id is `org.ctrlprivacy.<name>`, and everything is
released together at the version in `gradle.properties`, which CI sets from the release tag.
`README.md` explains how services consume it.

## Layout

`ctrlprivacy-build` is an included build (`pluginManagement { includeBuild(...) }` in
`settings.gradle.kts`) of precompiled Kotlin script plugins in `src/main/kotlin`, with the
Checkstyle, PMD and SpotBugs rule files in `src/main/resources/ctrlprivacy/`. Its
`generateBuildInfo` task writes `CtrlPrivacyBuildInfo` with the release version, which is how the
plugins find `ctrlprivacy-dependencies` at the same version outside this repo.

`ctrlprivacy-dependencies` is the BOM. Third-party versions live only in
`gradle/libs.versions.toml`; add one there only for a library neither Spring Boot nor Spring Cloud
manages, then add its constraint in `ctrlprivacy-dependencies/build.gradle.kts`.

## Conventions

The plugins hold every service to `-Werror`, NullAway on `@NullMarked` packages, Palantir
formatting, Checkstyle, PMD and SpotBugs with FindSecBugs. Changing a rule changes every service's
build at its next release, and tightening one is a breaking change.

Modules keep to one concern each. `ctrlprivacy-security` owns who the caller is and what they
may do: token verification, the 401/403 answers, method security and its denials, and `Caller`.
No other module references Spring Security or reads token claims. `ctrlprivacy-web` owns the shape
of HTTP requests and answers (problem details for business and MVC errors, `ApiResponse`, client
IP resolution). `ctrlprivacy-core` owns the data model and auditing, asking `Caller` for the
auditor.

Every module has a `README.md` following the component template in the workspace's
`docs/README template.md`, which `checkReadme` enforces at a minimum length. Values are immutable
Lombok classes, and every package has a `package-info.java` with `@NullMarked`.
