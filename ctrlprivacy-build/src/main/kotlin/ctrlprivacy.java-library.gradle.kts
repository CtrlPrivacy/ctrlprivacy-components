// The convention every ctrlprivacy-components library applies: the ctrlprivacy.java base, published
// to GitHub Packages with sources and Javadoc.

plugins {
    `java-library`
    id("ctrlprivacy.java")
    id("ctrlprivacy.publishing")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}
