# ctrlprivacy-build

## At a glance

| | |
|---|---|
| Artifact | `org.ctrlprivacy:ctrlprivacy-build` (Gradle plugins `ctrlprivacy.*`) |
| Package | Precompiled script plugins in `src/main/kotlin` |
| Status | Experimental |
| Compatibility | Gradle (wrapper version in `gradle/wrapper`), Java 25 |
| Auto-configuration | None |
| Depends on | `ctrlprivacy-dependencies` (versions), Error Prone, NullAway, Spotless, Checkstyle, PMD, SpotBugs |

## Purpose

Gradle convention plugins every Ctrl Privacy Java project applies, so every component and service
builds with the same toolchain, quality gates and publishing. It is a separate included build
(`pluginManagement { includeBuild("ctrlprivacy-build") }`), published on its own with
`./gradlew :ctrlprivacy-build:publish`.

## Responsibilities

**Does**

- Set the Java 25 toolchain and read every version from `ctrlprivacy-dependencies`.
- Run the quality gates: Error Prone with NullAway, Spotless (Palantir), Checkstyle, PMD, SpotBugs
  with FindSecBugs.
- Set up Lombok, MapStruct, JUnit and AssertJ.
- Split tests tagged `integration` into their own `integrationTest` task.
- Build components as libraries with sources and Javadoc, and services as executable Boot jars.
- Publish to GitHub Packages.
- Fail the build for a project without a README (`checkReadme`).

**Does not**

- Pin versions itself (`ctrlprivacy-dependencies`).
- Check README sections against the template; `checkReadme` only checks a title and length.

## Requirements

### Functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| BUILD-FR-001 | `checkReadme` must fail a project with no `README.md`, or one without a `# ` title or with under 10 non-blank lines. A published component checks its own directory; any other project checks its repository's root. | Engineering | Not yet verified |
| BUILD-FR-002 | Any compiler warning, NullAway error, format, Checkstyle, PMD or SpotBugs violation must fail the build. | Engineering | Not yet verified |
| BUILD-FR-003 | An unmapped MapStruct target must fail compilation. | Engineering | Not yet verified |
| BUILD-FR-004 | Locale-sensitive calls (`toLowerCase()` without a locale, …) must be compile errors. | Engineering | Not yet verified |
| BUILD-FR-005 | `org.ctrlprivacy.service` must build one executable jar named after the module, with `build-info` for `/actuator/info`. | Engineering | Not yet verified |
| BUILD-FR-006 | `test` must never run a test tagged `integration`, and `integrationTest` must run only those. | Engineering | Not yet verified |

### Non-functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| BUILD-NFR-001 | Every repository must get the same rule sets, shipped inside this build. | Engineering | Not yet verified |

The plugins have no tests of their own; every module in ctrlprivacy-components building with them
is the current check.

## Contract

Every Ctrl Privacy Java repository relies on these. Changing them is a breaking change.

- Plugin IDs: `org.ctrlprivacy.java`, `org.ctrlprivacy.java-library`, `org.ctrlprivacy.service`,
  `org.ctrlprivacy.publishing`, `org.ctrlprivacy.readme`.
- The `checkReadme` task and its place in `check`.
- The `integrationTest` task and the `integration` tag.
- The rule sets in `src/main/resources/ctrlprivacy/`. Tightening a rule is breaking, because it can
  fail builds that passed.

## Installation

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://maven.pkg.github.com/CtrlPrivacy/ctrlprivacy-components") {
            name = "GitHubPackages"
            credentials(PasswordCredentials::class)
        }
        gradlePluginPortal()
    }
}

// web/build.gradle.kts
plugins {
    id("org.ctrlprivacy.service") version "<version>"
}

// core/build.gradle.kts
plugins {
    id("org.ctrlprivacy.java") version "<version>"
}
```

## Configuration

None beyond the plugin you apply. Publishing reads the `GitHubPackagesUsername` and
`GitHubPackagesPassword` Gradle properties (`ORG_GRADLE_PROJECT_…` in CI).

## Usage

| Plugin | For | Adds |
|---|---|---|
| `org.ctrlprivacy.java` | the base of the others, and a service's non-application modules (`core`) | Java 25 toolchain; versions only from `ctrlprivacy-dependencies`; Error Prone with NullAway (JSpecify mode, `-Werror`); Lombok and MapStruct; Palantir formatting (Spotless); Checkstyle; PMD; SpotBugs with FindSecBugs; JUnit and AssertJ; `integrationTest`; the README check |
| `org.ctrlprivacy.java-library` | a shared component | `org.ctrlprivacy.java`, `java-library`, sources and Javadoc jars, publishing |
| `org.ctrlprivacy.service` | a service's application module (`web`) | `org.ctrlprivacy.java` and the Spring Boot plugin, built into one executable jar, `<module>.jar`, with `build-info` |
| `org.ctrlprivacy.publishing` | publishing | Maven publication to GitHub Packages |
| `org.ctrlprivacy.readme` | documentation | `checkReadme`, part of `check`: a component's own README, or a service's root README |

### Quality gates

- **Compiler:** `-Xlint:all -Werror`; NullAway on production code, where packages are `@NullMarked`.
  Mapper targets must all be mapped (`mapstruct.unmappedTargetPolicy=ERROR`), and mappers are
  Spring components by default.
- **Format:** `./gradlew spotlessApply` fixes it.
- **Checkstyle, PMD, SpotBugs:** the rule sets ship inside this build (`src/main/resources/ctrlprivacy/`).
  Tests are held to Checkstyle's limits and PMD's cleanup rules only (`pmd-test.xml`). SpotBugs runs
  on production code.
- **Locale-sensitive calls** are compile errors.

### Tests

`test` runs every test except those tagged `integration`, so it needs nothing running and is what
CI runs. `integrationTest` runs only the tagged tests (Testcontainers, a full Spring context
against a real database), from the same test sources. Neither is part of the other, and neither
fails when a module has no tests of its kind.

### Versions

`org.ctrlprivacy.java` reads every version from `ctrlprivacy-dependencies`: the local project inside
ctrlprivacy-components, and the release with the same version as these plugins anywhere else.
Declare dependencies without versions.

### This README

`ctrlprivacy-build` is the one project that can't apply its own plugin, so `checkReadme` doesn't
run on this file; it's kept to the template by hand.

## Security

- **Threats:** insecure code patterns reaching production; a compromised or unexpected plugin or
  dependency version.
- **Untrusted input:** none at runtime; it runs in the build.
- **Defaults:** FindSecBugs on every production class; warnings are errors.
- **Protections:** one rule set for every repository. Limits: static analysis finds patterns, not
  every vulnerability; dependency scanning is left to Dependabot and CI.
- **Reporting:** report a suspected vulnerability privately to the maintainers, not in a public
  issue.

## Testing

```bash
./gradlew build
```

Building ctrlprivacy-components applies the plugins to every module.

## Standards and references

- [Gradle convention plugins](https://docs.gradle.org/current/samples/sample_convention_plugins.html)
- [Error Prone](https://errorprone.info/) and [NullAway](https://github.com/uber/NullAway)
- [Palantir Java Format](https://github.com/palantir/palantir-java-format)
- [FindSecBugs](https://find-sec-bugs.github.io/)

## Status

**Experimental.** Pre-1.0 (`0.1.0-SNAPSHOT`); gates are still being tuned as the services move on
to them.

## Changelog

See the [ctrlprivacy-components releases](https://github.com/CtrlPrivacy/ctrlprivacy-components/releases).
A new or tightened rule is breaking.
