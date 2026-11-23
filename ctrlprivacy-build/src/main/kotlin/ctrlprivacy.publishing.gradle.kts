// Maven publication to GitHub Packages. Credentials come from the GitHubPackagesUsername and
// GitHubPackagesPassword Gradle properties; CI sets them as ORG_GRADLE_PROJECT_* variables.

plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pluginManager.withPlugin("java-library") {
                from(components["java"])
                versionMapping {
                    usage("java-api") { fromResolutionOf("runtimeClasspath") }
                    usage("java-runtime") { fromResolutionResult() }
                }
            }
            pluginManager.withPlugin("java-platform") {
                from(components["javaPlatform"])
            }
            pom {
                name = project.name
                description = provider { project.description }
                url = "https://github.com/CtrlPrivacy/ctrlprivacy-components"
                scm {
                    url = "https://github.com/CtrlPrivacy/ctrlprivacy-components"
                    connection = "scm:git:https://github.com/CtrlPrivacy/ctrlprivacy-components.git"
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/CtrlPrivacy/ctrlprivacy-components")
            credentials(PasswordCredentials::class)
        }
    }
}
