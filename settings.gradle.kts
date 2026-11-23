pluginManagement {
    includeBuild("ctrlprivacy-build")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "ctrlprivacy-components"

include(
    "ctrlprivacy-core",
    "ctrlprivacy-dependencies",
    "ctrlprivacy-security",
    "ctrlprivacy-web",
)
