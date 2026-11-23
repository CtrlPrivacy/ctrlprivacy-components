// The convention every ctrlprivacy-components library applies: the org.ctrlprivacy.java base, published
// to GitHub Packages with sources and Javadoc.

plugins {
    `java-library`
    id("org.ctrlprivacy.java")
    id("org.ctrlprivacy.publishing")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}
