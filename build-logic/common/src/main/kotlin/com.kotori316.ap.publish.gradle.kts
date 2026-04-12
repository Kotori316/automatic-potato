import com.kotori316.plugin.cf.CallVersionCheckFunctionTask
import com.kotori316.plugin.cf.CallVersionFunctionTask

plugins {
    id("maven-publish")
    id("signing")
    id("net.fabricmc.fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
    id("com.kotori316.plugin.cf")
}

val modId: String by project
val minecraftVersion: String by project
val releaseDebug = (System.getenv("RELEASE_DEBUG") ?: "true").toBoolean()
val hasGpgSignature = project.hasProperty("signing.keyId") &&
        project.hasProperty("signing.password") &&
        project.hasProperty("signing.secretKeyRingFile")
val catalog: VersionCatalog = project.versionCatalogs.named("libs")

signing {
    sign(publishing.publications)
}

tasks {
    val jksSignJar = register("jksSignJar", JarSignTask::class) {
        onlyIf {
            project.hasProperty("jarSign.keyAlias") &&
                    project.hasProperty("jarSign.keyLocation") &&
                    project.hasProperty("jarSign.storePass")
        }
        jarFile = tasks.jar.flatMap { it.archiveFile }
        keyAlias = project.findProperty("jarSign.keyAlias") as? String ?: ""
        keyStore = project.findProperty("jarSign.keyLocation") as? String ?: ""
        storePass = project.findProperty("jarSign.storePass") as? String ?: ""
    }
    tasks.jar {
        finalizedBy(jksSignJar)
    }
    withType(Sign::class) {
        onlyIf { hasGpgSignature }
    }
    withType(AbstractPublishToMaven::class) {
        if (hasGpgSignature) {
            dependsOn("signRemapJar")
        }
    }

    register("registerVersion", CallVersionFunctionTask::class) {
        functionEndpoint = CallVersionFunctionTask.readVersionFunctionEndpoint(project)
        gameVersion = minecraftVersion
        platform = project.name
        platformVersion = catalog.findVersion("fabric_loader").map { it.requiredVersion }.get()
        modName = modId
        changelog = "Version ${project.version}"
        homepage = "https://modrinth.com/project/automatic-potato"
        isDryRun = releaseDebug
    }
    register("checkReleaseVersion", CallVersionCheckFunctionTask::class) {
        gameVersion = minecraftVersion
        platform = project.name
        modName = modId
        version = project.version.toString()
        failIfExists = !releaseDebug
    }
}

publishMods {
    dryRun = releaseDebug
    type = STABLE
    file = tasks.jar.flatMap { it.archiveFile }
    additionalFiles = files(
        tasks.named("sourcesJar")
    )
    modLoaders = listOf(project.name)
    displayName = "${project.version}-${project.name}"
    changelog = "See https://github.com/Kotori316/automatic-potato"

    curseforge {
        accessToken = (
                project.findProperty("curseforge_additional-enchanted-miner_key")
                    ?: System.getenv("CURSE_TOKEN")
                    ?: "") as String
        projectId = "973884"
        minecraftVersionRange {
            start = minecraftVersion
            end = "latest"
        }
    }

    modrinth {
        accessToken = (project.findProperty("modrinthToken") ?: System.getenv("MODRINTH_TOKEN") ?: "") as String
        projectId = "fLArsyMM"
        minecraftVersionRange {
            start = minecraftVersion
            end = "latest"
        }
    }
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            artifactId = base.archivesName.get()
            from(components.getAt("java"))
        }
    }

    repositories {
        val u = project.findProperty("maven_username") as? String ?: System.getenv("MAVEN_USERNAME") ?: ""
        val p = project.findProperty("maven_password") as? String ?: System.getenv("MAVEN_PASSWORD") ?: ""
        if (u != "" && p != "") {
            maven {
                name = "kotori316-maven"
                // For users: Use https://maven.kotori316.com to get artifacts
                url = uri("https://maven2.kotori316.com/production/maven")
                credentials {
                    username = u
                    password = p
                }
            }
        }
    }
}
