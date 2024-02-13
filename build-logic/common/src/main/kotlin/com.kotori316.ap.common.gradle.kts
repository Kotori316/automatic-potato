plugins {
    java
}

base {
    archivesName = "VersionCheckerMod"
    group = "com.kotori316"
    version = project.property("modVersion")!!
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

val modId: String by project
// Fixed. Use 1.16.5
val minecraftVersion: String by project

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val version = project.version.toString()
    inputs.property("modVersion", version)
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
