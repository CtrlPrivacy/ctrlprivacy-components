// The base every Ctrl Privacy Java project applies, through org.ctrlprivacy.java-library or
// org.ctrlprivacy.service: the Java 25 toolchain, ctrlprivacy-dependencies for every version, Error Prone
// with NullAway, Lombok and MapStruct, Palantir formatting, Checkstyle, PMD and SpotBugs with
// FindSecBugs, the test stack (JUnit, AssertJ), and the integrationTest task.

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.ctrlprivacy.build.CtrlPrivacyBuildInfo

plugins {
    java
    checkstyle
    pmd
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
    id("org.ctrlprivacy.readme")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Versions come only from ctrlprivacy-dependencies. Inside ctrlprivacy-components it's the local
// project; anywhere else it's the release with the same version as these conventions.
val ctrlprivacyManagement = configurations.dependencyScope("ctrlprivacyManagement")
configurations.configureEach {
    if (isCanBeResolved && name != ctrlprivacyManagement.name) {
        extendsFrom(ctrlprivacyManagement.get())
    }
}

dependencies {
    val bom = rootProject.findProject(":ctrlprivacy-dependencies")?.let { project(it.path) }
        ?: create("org.ctrlprivacy:ctrlprivacy-dependencies:${CtrlPrivacyBuildInfo.VERSION}")
    ctrlprivacyManagement(platform(bom))

    // Libraries export JSpecify's annotations with their API; a service only compiles against them.
    add(if (pluginManager.hasPlugin("java-library")) "api" else "implementation", "org.jspecify:jspecify")
    implementation("com.google.errorprone:error_prone_annotations")

    // Lombok writes the value classes (@Value, @Builder) for entities and DTOs.
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // MapStruct writes the entity ↔ DTO mappers. The binding makes Lombok run first, so MapStruct
    // sees the getters and constructors Lombok writes.
    implementation("org.mapstruct:mapstruct")
    annotationProcessor("org.mapstruct:mapstruct-processor")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor")
    testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding")

    errorprone("com.google.errorprone:error_prone_core")
    errorprone("com.uber.nullaway:nullaway")

    checkstyle("com.puppycrawl.tools:checkstyle")
    pmd("net.sourceforge.pmd:pmd-java")
    pmd("net.sourceforge.pmd:pmd-ant")
    spotbugs("com.github.spotbugs:spotbugs")
    // Non-transitive, so platform constraints don't reach it; the version comes from the same catalog.
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:${CtrlPrivacyBuildInfo.FINDSECBUGS}")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    // Lombok reads lombok.config while compiling, so changing it must recompile.
    rootProject.file("lombok.config").takeIf { it.exists() }?.let {
        inputs.file(it).withPropertyName("lombokConfig").withPathSensitivity(PathSensitivity.RELATIVE)
    }
    options.encoding = "UTF-8"
    options.release = 25
    // -parameters keeps constructor parameter names, which Spring Data needs to map documents onto
    // immutable entities.
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing", "-Werror"))
    // Every mapper maps every target property, or the build fails.
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.unmappedTargetPolicy=ERROR",
        "-Amapstruct.defaultComponentModel=spring",
        "-Amapstruct.suppressGeneratorTimestamp=true",
    ))
    options.errorprone {
        disableWarningsInGeneratedCode = true
        check("NullAway", if (name.contains("test", ignoreCase = true)) CheckSeverity.OFF else CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
        // MapStruct's generated mappers return null for a null source; they're marked @Generated.
        option("NullAway:TreatGeneratedAsUnannotated", "true")
        check("StringCaseLocaleUsage", CheckSeverity.ERROR)
        check("DefaultLocale", CheckSeverity.ERROR)
    }
}

// The rules ship inside ctrlprivacy-build, so every repository applying it gets the same ones; any
// violation fails the build.
fun ctrlprivacyRules(name: String) = resources.text.fromString(
    object {}.javaClass.getResource("/ctrlprivacy/$name")!!.readText())

checkstyle {
    config = ctrlprivacyRules("checkstyle.xml")
    maxWarnings = 0
    isIgnoreFailures = false
}

pmd {
    ruleSets = emptyList()
    ruleSetConfig = ctrlprivacyRules("pmd.xml")
    isConsoleOutput = true
    isIgnoreFailures = false
}

// Tests are held to Checkstyle's nesting and complexity limits, and to PMD's cleanup rules only.
tasks.withType<Pmd>().matching { it.name == "pmdTest" }.configureEach {
    ruleSetConfig = ctrlprivacyRules("pmd-test.xml")
}

// Bug patterns, and security bug patterns with FindSecBugs. Production code only.
spotbugs {
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    excludeFilter = layout.buildDirectory.file("ctrlprivacy/spotbugs-exclude.xml")
}
val spotbugsExclude = tasks.register("ctrlprivacySpotbugsExclude") {
    val target = layout.buildDirectory.file("ctrlprivacy/spotbugs-exclude.xml")
    val content = object {}.javaClass.getResource("/ctrlprivacy/spotbugs-exclude.xml")!!.readText()
    outputs.file(target)
    doLast { target.get().asFile.apply { parentFile.mkdirs() }.writeText(content) }
}
tasks.withType<SpotBugsTask>().configureEach {
    dependsOn(spotbugsExclude)
    enabled = name == "spotbugsMain"
    reports.create("html") { required = true }
    reports.create("xml") { required = true }
}

// Tests tagged "integration" (@IntegrationTest in the services) need a live integration such as
// Docker. The default test task leaves them out, so it runs anywhere; integrationTest runs only
// them, from the same test sources.
val integrationTag = "integration"

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags(integrationTag)
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs tests tagged @IntegrationTest, which depend on live integrations such as Docker."
    group = "verification"
    useJUnitPlatform {
        includeTags(integrationTag)
    }
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    shouldRunAfter("test")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

spotless {
    java {
        palantirJavaFormat("2.102.0")
    }
}
