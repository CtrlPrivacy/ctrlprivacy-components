plugins {
    id("ctrlprivacy.java-library")
}

description = "Ctrl Privacy core: the base entity every document extends (identity and audit trail), the base response and auditing"

dependencies {
    // BaseEntity carries Spring Data's mapping and auditing annotations.
    api("org.springframework.data:spring-data-mongodb")
    // Public auto-configuration classes carry its annotations, so it is part of this module's API.
    api("org.springframework.boot:spring-boot-autoconfigure")
    // EntityMapperConfig is a MapStruct @MapperConfig the services' mappers reference.
    api("org.mapstruct:mapstruct")
    // The auditor asks ctrlprivacy-security's Caller who the caller is, when the service has it;
    // core never reads tokens itself.
    compileOnly(project(":ctrlprivacy-security"))

    testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    testImplementation(project(":ctrlprivacy-security"))
    testImplementation("org.springframework.security:spring-security-oauth2-jose")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
