// Every Ctrl Privacy project documents what it does: a README.md with a title and some substance. A
// stub fails `check`, so a new component can't be added without one.
//
// A published component (java-library or java-platform) documents itself in its own directory. A
// service's modules (core, web) are documented once, by the README at the root of the service's
// repository, so that's the one they check.

val isPublished = pluginManager.hasPlugin("java-library") || pluginManager.hasPlugin("java-platform")

val checkReadme = tasks.register("checkReadme") {
    group = "verification"
    description = "Fails if the project has no README.md, or only a stub."
    val readme = if (isPublished) {
        layout.projectDirectory.file("README.md").asFile
    } else {
        isolated.rootProject.projectDirectory.file("README.md").asFile
    }
    val owner = if (isPublished) project.name else isolated.rootProject.name
    inputs.files(readme).optional()
    doLast {
        if (!readme.isFile) {
            throw GradleException("$owner has no README.md. Document what it does.")
        }
        val lines = readme.readLines().filter { it.isNotBlank() }
        if (lines.none { it.startsWith("# ") } || lines.size < 10) {
            throw GradleException(
                "$owner's README.md is a stub: it needs a '# ' title and at least 10 lines.")
        }
    }
}

tasks.matching { it.name == "check" }.configureEach { dependsOn(checkReadme) }
