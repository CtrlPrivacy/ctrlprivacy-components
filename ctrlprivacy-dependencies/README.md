# ctrlprivacy-dependencies

## At a glance

| | |
|---|---|
| Artifact | `org.ctrlprivacy:ctrlprivacy-dependencies` (platform / BOM) |
| Package | None |
| Status | Experimental |
| Compatibility | Java 25, Spring Boot 4.2, Spring Cloud 2026.0 |
| Auto-configuration | None |
| Depends on | Spring Boot BOM, Spring Cloud BOM, AWS SDK BOM |

## Purpose

The release-train BOM. Every `ctrlprivacy-*` module and every third-party version Ctrl Privacy uses
is pinned here, so a service declares dependencies without versions and every backend stays in
step.

## Responsibilities

**Does**

- Import Spring Boot's, Spring Cloud's and the AWS SDK's BOMs whole.
- Constrain every `ctrlprivacy-*` library module to the release version.
- Pin what those BOMs don't manage, from `gradle/libs.versions.toml`.

**Does not**

- Add dependencies to a project; it only sets versions.
- Configure plugins or quality gates (`ctrlprivacy-build`).

## Requirements

### Functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| DEPS-FR-001 | Every `ctrlprivacy-*` library module in `settings.gradle.kts` must have a constraint at the release version. | Engineering | Not yet verified |
| DEPS-FR-002 | Every dependency a component or service declares must resolve without a version. | Engineering | `./gradlew build` (compiles every module without versions) |

### Non-functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| DEPS-NFR-001 | Spring Cloud must be the release train built for the imported Spring Boot version. | Engineering | Not yet verified |

## Contract

Every service relies on it. A major upgrade of an imported BOM is a breaking change.

- The artifact coordinate and that it is a Gradle platform.
- The imported BOMs and the pinned libraries listed under Usage.

## Installation

The `ctrlprivacy.java` convention plugin applies it; nothing else is needed. Without the plugin:

```kotlin
dependencies {
    implementation(platform("org.ctrlprivacy:ctrlprivacy-dependencies:<version>"))
}
```

## Configuration

None. Versions live in `gradle/libs.versions.toml`.

## Usage

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")   // no version
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
}
```

What it manages:

- **Imported BOMs:** Spring Boot (Spring, Mongo, Kafka, Jackson, Lombok, Caffeine,
  Testcontainers, JUnit, AssertJ), Spring Cloud (Gateway), AWS SDK (S3 for Cloudflare R2).
- **`ctrlprivacy-*` modules:** every library module in this repository, once there is one.
- **Pinned build tooling:** Error Prone, NullAway, MapStruct and the Lombok binding, Checkstyle,
  PMD, SpotBugs and FindSecBugs.
- **Pinned libraries:** springdoc-openapi, bucket4j, GeoIP2, commons-compress, Avro, the Confluent
  Avro serializer, and jjwt.

### Adding a module

A new library component adds one line to the `constraints` block in `build.gradle.kts`
(`api(project(":ctrlprivacy-<name>"))`), and one `include` in `settings.gradle.kts`.

### Upgrading Spring Boot

Change `spring-boot` in `gradle/libs.versions.toml`, and `spring-cloud` to the release train built
for it.

## Security

- **Threats:** a vulnerable transitive version; services drifting onto different versions of a
  library.
- **Untrusted input:** none.
- **Defaults:** every version comes from one place.
- **Protections:** one upgrade here moves every service. Limits: it doesn't scan for known
  vulnerabilities; that's left to Dependabot and dependency scanning in CI.
- **Reporting:** report a suspected vulnerability privately to the maintainers, not in a public
  issue.

## Testing

```bash
./gradlew build
```

There are no tests of its own; every module building against it is the check.

## Standards and references

- [Gradle platforms](https://docs.gradle.org/current/userguide/platforms.html)
- [Spring Boot dependency versions](https://docs.spring.io/spring-boot/appendix/dependency-versions/)

## Status

**Experimental.** Pre-1.0 (`0.1.0-SNAPSHOT`) while the services move on to it.

## Changelog

See the [ctrlprivacy-components releases](https://github.com/CtrlPrivacy/ctrlprivacy-components/releases).
A major upgrade of an imported BOM is breaking.
