plugins {
    id("org.ctrlprivacy.java-library")
}

description = "Ctrl Privacy web: problem detail error answers, the ApiResponse envelope and client IP resolution behind trusted proxies"

dependencies {
    // ErrorCode carries an HttpStatus, and errors are answered as ProblemDetail.
    api("org.springframework:spring-web")
    // Public auto-configuration classes carry its annotations, so it is part of this module's API.
    api("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.slf4j:slf4j-api")
    // Every service brings its own Spring MVC and servlet container; a core module that only throws
    // CtrlPrivacyException needs neither.
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
