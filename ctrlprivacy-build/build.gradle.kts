import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

// ctrlprivacy-build is released with the train, so it shares the monorepo's group and version.
val train = Properties().apply { file("../gradle.properties").reader().use(::load) }
group = train.getProperty("group")
version = train.getProperty("version")
description = "Ctrl Privacy's Gradle convention plugins"

// The same repository as ctrlprivacy.publishing, which can't be applied to the build that defines it.
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/CtrlPrivacy/ctrlprivacy-components")
            credentials(PasswordCredentials::class)
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val catalog = file("../gradle/libs.versions.toml").readText()
fun catalogVersion(name: String) =
    Regex("""(?m)^$name = "([^"]+)"""").find(catalog)?.groupValues?.get(1)
        ?: error("$name is missing from gradle/libs.versions.toml")

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.11.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:5.1.1")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.5.13")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:${catalogVersion("spring-boot")}")
}

// Lets the conventions find ctrlprivacy-dependencies at the same version when applied outside the
// monorepo, and gives them the versions of tools that can't take theirs from it.
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val output = layout.buildDirectory.dir("generated/ctrlprivacy-build-info")
    val trainVersion = version.toString()
    val findsecbugs = catalogVersion("findsecbugs")
    inputs.property("version", trainVersion)
    inputs.property("findsecbugs", findsecbugs)
    outputs.dir(output)
    doLast {
        val file = output.get().file("org/ctrlprivacy/build/CtrlPrivacyBuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package org.ctrlprivacy.build
            |
            |internal object CtrlPrivacyBuildInfo {
            |    const val VERSION = "$trainVersion"
            |
            |    /** spotbugsPlugins is non-transitive, so it can't take its version from ctrlprivacy-dependencies. */
            |    const val FINDSECBUGS = "$findsecbugs"
            |}
            |""".trimMargin(),
        )
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateBuildInfo)
}
