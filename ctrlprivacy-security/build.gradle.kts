plugins {
    id("ctrlprivacy.java-library")
    `java-test-fixtures`
}

description = "Ctrl Privacy security: Hydra access token verification, permission authorities and default routes for resource servers"

dependencies {
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    // Public auto-configuration classes carry its annotations, so it is part of this module's API.
    api("org.springframework.boot:spring-boot-autoconfigure")
    // Problem details for 401 and 403 answers.
    api("org.springframework:spring-web")
    implementation("tools.jackson.core:jackson-databind")
    // Every service brings its own servlet container.
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    // The actuator chain is only set up when the service has the actuator.
    compileOnly("org.springframework.boot:spring-boot-actuator-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-security")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // TestTokens, published as test fixtures: testImplementation(testFixtures("org.ctrlprivacy:ctrlprivacy-security")).
    testFixturesApi("com.nimbusds:nimbus-jose-jwt")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
