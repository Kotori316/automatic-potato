plugins {
    java
}

base {
    archivesName = "VersionCheckerMod"
    group = "com.kotori316"
    version = "1.0.0"
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

val modId: String = "kotori316_version_checker"
// Fixed. Use 1.16.5
val minecraftVersion: String = "1.16.5"

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val version = project.version.toString()
    inputs.property("version", version)
    listOf("fabric.mod.json", "META-INF/mods.toml").forEach { fileName ->
        filesMatching(fileName) {
            expand(
                "version" to version,
                "update_url" to "https://version.kotori316.com/get-version/${minecraftVersion}/${project.name}/${modId}",
                "mc_version" to minecraftVersion,
            )
        }
    }
}
