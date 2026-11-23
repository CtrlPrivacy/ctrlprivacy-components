# ctrlprivacy-components

The shared Java components of Ctrl Privacy's Spring Boot APIs. Each API (activity, resource, auth,
gateway) is a Spring Boot application on top of these modules, which hold everything the APIs do
the same way: dependency versions, the build and its quality gates, and, as they're added, Hydra
token verification and the shared web layer. A service adds a plugin or a jar and gets the
feature, set up the Ctrl Privacy way.

Every module is published to GitHub Packages as `org.ctrlprivacy:<module>` and released together,
at one version, through `ctrlprivacy-dependencies`. Java 25, Spring Boot 4.2.

## Modules

Each module's README covers its purpose, requirements, contract, configuration and usage.

| Group | Module | What it is |
|---|---|---|
| Build | [`ctrlprivacy-build`](ctrlprivacy-build/README.md) | Gradle convention plugins: toolchain, quality gates, tests, publishing, README check |
| | [`ctrlprivacy-dependencies`](ctrlprivacy-dependencies/README.md) | The release-train BOM: every `ctrlprivacy-*` module and third-party version |
| Platform | [`ctrlprivacy-core`](ctrlprivacy-core/README.md) | `BaseEntity` (`_id`, `_key`, audit trail), `BaseResponse`, MongoDB auditing |
| | [`ctrlprivacy-security`](ctrlprivacy-security/README.md) | Hydra access token verification, `permissions` as authorities, default routes, 401/403 problems |
| | [`ctrlprivacy-web`](ctrlprivacy-web/README.md) | Problem detail error answers with a `code`, the `ApiResponse` envelope, client IP resolution behind trusted proxies |

## Getting started

### Prerequisites

- **JDK 25.** The Gradle toolchain and the daemon (`gradle/gradle-daemon-jvm.properties`) ask
  for it; Temurin is what CI uses.
- **Docker**, for the integration tests (Testcontainers). Only `integrationTest` needs it.
- **A GitHub token with `read:packages`**, to resolve published `ctrlprivacy-*` versions, and with
  `write:packages` if you'll publish from your machine. Put it in `~/.gradle/gradle.properties`:

  ```properties
  GitHubPackagesUsername=<your GitHub user>
  GitHubPackagesPassword=<token>
  ```

- **IDE:** enable annotation processing (Lombok, MapStruct). The Palantir Java Format plugin keeps
  formatting in step with `spotlessApply`.

### Build

```bash
git clone https://github.com/CtrlPrivacy/ctrlprivacy-components.git
cd ctrlprivacy-components
./gradlew build :ctrlprivacy-build:build
```

`build` runs the formatting check (fix it with `./gradlew spotlessApply`), Error Prone and NullAway,
Checkstyle, PMD, SpotBugs, the README check, and every test except the integration tests. Any
violation fails the build. `./gradlew integrationTest` runs the integration tests (tagged
`integration`) with Docker. One module on its own: `./gradlew :ctrlprivacy-dependencies:build`.

### Try a change in a service before releasing it

Publish the current version (`0.1.0-SNAPSHOT` in `gradle.properties`) to your local Maven
repository, plugins included:

```bash
./gradlew publishToMavenLocal :ctrlprivacy-build:publishToMavenLocal
```

Then point the service at it: add `mavenLocal()` first in its `pluginManagement` and
`dependencyResolutionManagement` repositories, and use `0.1.0-SNAPSHOT` as the plugin version.
Republish after each change; remove `mavenLocal()` before merging the service.

## Using the components in a service

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

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.github.com/CtrlPrivacy/ctrlprivacy-components") {
            name = "GitHubPackages"
            credentials(PasswordCredentials::class)
        }
        // Confluent's Avro serializer (resource-service) is only published here.
        maven("https://packages.confluent.io/maven/") {
            content { includeGroupByRegex("io\\.confluent(\\..*)?") }
        }
    }
}

// core/build.gradle.kts
plugins {
    id("ctrlprivacy.java") version "<version>"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")   // no versions
}

// web/build.gradle.kts
plugins {
    id("ctrlprivacy.service") version "<version>"
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

The `ctrlprivacy.*` plugins bring in `ctrlprivacy-dependencies` at their own version, so the plugin
version is the only version a service sets.

**CI in other repositories:** this repository is public, so any repository's own `GITHUB_TOKEN`
can read the packages. GitHub Packages still wants a token even for public Maven packages, so the
workflow passes it, and the job needs `packages: read`:

```yaml
permissions:
  contents: read
  packages: read
env:
  ORG_GRADLE_PROJECT_GitHubPackagesUsername: ${{ github.actor }}
  ORG_GRADLE_PROJECT_GitHubPackagesPassword: ${{ secrets.GITHUB_TOKEN }}
```

### How an API is laid out

Two Gradle modules. `core` holds the business logic (entities, repositories, services, mappers,
policies and rules) and applies `ctrlprivacy.java`. `web` holds what faces the client
(controllers, security and Swagger config, the application class), depends on `core`, and applies
`ctrlprivacy.service`, which builds `web/build/libs/web.jar` for the image. `core` never depends on
`web`.

## Contributing

- **Branches:** `<name>/<few-word-desc>`, such as `dylan/ory-gateway-auth`. Open a pull request to
  `main`.
- **Before a pull request:** `./gradlew spotlessApply build :ctrlprivacy-build:build`, and
  `integrationTest` with Docker running for any module that has integration tests.
- **Breaking changes:** anything in a module's Contract or Configuration section. Call them out in
  the pull request; services pick them up at the next release.
- **Values** are immutable Lombok classes (`@Value`, `@Builder`), not records. Every package has a
  `package-info.java` with `@NullMarked`.

### Adding a module

1. Create `ctrlprivacy-<name>/build.gradle.kts` applying `ctrlprivacy.java-library`, with a
   `description`.
2. `include("ctrlprivacy-<name>")` in `settings.gradle.kts`.
3. Add a constraint for it in `ctrlprivacy-dependencies/build.gradle.kts`.
4. Put classes one package level under `org.ctrlprivacy.<name>`, each package with a `@NullMarked`
   `package-info.java`, and `@AutoConfiguration` classes in `autoconfigure`.
5. Register auto-configurations in
   `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
6. Write its `README.md` from the component template (`docs/README template.md` in the workspace),
   and add it to the table above.

## Documentation

Every module's README follows the same sections: At a glance, Purpose, Responsibilities,
Requirements, Contract, Installation, Configuration, Usage, Security, Testing, Standards and
references, Status, Changelog.

- **Requirements** are `| ID | Requirement | Owner | Verified by |` tables, functional then
  non-functional, with IDs prefixed per module (`DEPS-FR-001`). Never reuse or renumber an ID; add
  the next one, and strike through one that's retired. "Verified by" names the test class, or says
  "Not yet verified".
- `checkReadme` (part of `check`) fails a module without a README, or with only a stub. It doesn't
  check the sections; reviews do.

## Publishing

CI (`.github/workflows/workflow.yaml`) tests every pull request and push to `main` (without the
integration tests), and runs the static analysis and Javadoc on pull requests only. Only GitHub
releases publish to GitHub Packages, and the tag sets the version (don't edit it by hand):

- a release tagged `v1.2.3` publishes `1.2.3`; release candidates (`v1.2.0-rc.1`) are released from
  `main`
- snapshots (`v1.2.0-SNAPSHOT`) are releases on a PR branch. A `-SNAPSHOT` version can be published
  again; any other version can't be replaced once published
- a manual run with **republish** publishes the version given

To publish from a machine instead:

```bash
ORG_GRADLE_PROJECT_GitHubPackagesUsername=<user> \
ORG_GRADLE_PROJECT_GitHubPackagesPassword=<token> \
./gradlew publish :ctrlprivacy-build:publish
```

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `401` or `Could not resolve org.ctrlprivacy:…` | Missing or expired GitHub token, or it lacks `read:packages`. Check `~/.gradle/gradle.properties`; in another repo's CI, check the job has `packages: read` and passes `GITHUB_TOKEN`. |
| `Could not resolve io.confluent:…` | The Confluent repository is missing from `dependencyResolutionManagement`. |
| `Cannot find a Java installation … languageVersion=25` | JDK 25 isn't installed, or Gradle can't find it. Install Temurin 25. |
| A service still sees old component code | Republish with `publishToMavenLocal`, and check `mavenLocal()` comes first. |
| An integration test didn't run | `test` never runs tests tagged `integration`; run `./gradlew integrationTest` with Docker running. |
| NullAway finds nothing in a new package | The package has no `package-info.java` with `@NullMarked`. |
| `spotlessCheck` fails | Run `./gradlew spotlessApply`. |
| `checkReadme` fails | The module's README is missing, has no `# ` title, or has under 10 non-blank lines. |
