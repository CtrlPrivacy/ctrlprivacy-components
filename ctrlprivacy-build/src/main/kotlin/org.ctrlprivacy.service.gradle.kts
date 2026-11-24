// The convention every Ctrl Privacy API applies to the module that builds the application (web):
// the org.ctrlprivacy.java base, built into one executable Spring Boot jar. Services ship as images, not
// Maven artifacts, so nothing is published from here.

plugins {
    id("org.ctrlprivacy.java")
    id("org.springframework.boot")
}

// build-info.properties feeds /actuator/info.
springBoot {
    buildInfo()
}

// The image copies <module>/build/libs/<module>.jar, so the name carries no version.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "${project.name}.jar"
}

// The plain jar isn't shipped; only the executable one is.
tasks.named<Jar>("jar") {
    enabled = false
}
