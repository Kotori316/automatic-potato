plugins {
    alias(libs.plugins.publish.all)
}

version = project.property("modVersion")!!

val releaseDebug = (System.getenv("RELEASE_DEBUG") ?: "true").toBoolean()

publishMods {
    changelog = "See https://github.com/Kotori316/automatic-potato"
    dryRun = releaseDebug
    displayName = "v${project.version} for Minecraft ${libs.versions.minecraft.get()} to latest"
    type = if (project.version.toString().contains("SNAPSHOT")) BETA else STABLE

    val releaseJarFiles = getReleaseJarFiles()
    file = releaseJarFiles.first()
    additionalFiles.from(
        *releaseJarFiles.drop(1).toTypedArray()
    )

    github {
        repository = "Kotori316/automatic-potato"
        accessToken = System.getenv("REPO_TOKEN") ?: ""
        commitish = System.getenv("GITHUB_SHA") ?: "main"
        tagName = "v${project.version}"
    }
}

fun getReleaseJarFiles(): List<Provider<RegularFile>> {
    val list = mutableListOf<Provider<RegularFile>>()
    if (!(System.getenv("DISABLE_FABRIC") ?: "false").toBoolean()) {
        list.add(project(":fabric").tasks.named("remapJar", AbstractArchiveTask::class).flatMap { it.archiveFile })
        list.add(
            project(":fabric").tasks.named("remapSourcesJar", AbstractArchiveTask::class).flatMap { it.archiveFile })
    }
    if (list.isEmpty()) {
        // Dummy file
        list.add(project.layout.file(provider { rootProject.file("README.md") }))
    }
    return list
}
