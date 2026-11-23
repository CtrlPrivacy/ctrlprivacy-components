// The release-train BOM: every ctrlprivacy-* module and every third-party version Ctrl Privacy uses.
// Spring Boot's, Spring Cloud's and the AWS SDK's BOMs are imported whole; gradle/libs.versions.toml
// adds the rest.

plugins {
    `java-platform`
    id("ctrlprivacy.publishing")
    id("ctrlprivacy.readme")
}

description = "Ctrl Privacy dependency management: the release-train BOM"

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(platform(libs.spring.cloud.dependencies))
    api(platform(libs.aws.sdk.bom))

    constraints {
        // Every ctrlprivacy-* library at the release version.
        api(project(":ctrlprivacy-core"))
        api(project(":ctrlprivacy-security"))
        api(project(":ctrlprivacy-web"))

        // Build tooling, used by the ctrlprivacy.java conventions.
        api(libs.error.prone.annotations)
        api(libs.error.prone.core)
        api(libs.nullaway)
        api(libs.mapstruct)
        api(libs.mapstruct.processor)
        api(libs.lombok.mapstruct.binding)
        api(libs.checkstyle)
        api(libs.pmd.java)
        api(libs.pmd.ant)
        api(libs.spotbugs)
        api(libs.spotbugs.annotations)
        api(libs.findsecbugs)

        // Libraries the services use that Spring doesn't manage.
        api(libs.springdoc.openapi.starter.webmvc.ui)
        api(libs.bucket4j)
        api(libs.geoip2)
        api(libs.commons.compress)
        api(libs.avro)
        api(libs.kafka.avro.serializer)
        api(libs.jjwt.api)
        api(libs.jjwt.impl)
        api(libs.jjwt.jackson)
    }
}
