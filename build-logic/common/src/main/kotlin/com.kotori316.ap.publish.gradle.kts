import com.kotori316.plugin.cf.CallVersionCheckFunctionTask
import com.kotori316.plugin.cf.CallVersionFunctionTask

plugins {
    id("maven-publish")
    id("signing")
    id("fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
    id("com.kotori316.plugin.cf")
}

val modId: String by project
// Fixed. Use 1.16.5
val minecraftVersion: String by project
val releaseDebug = (System.getenv("RELEASE_DEBUG") ?: "true").toBoolean()
val hasGpgSignature = project.hasProperty("signing.keyId") &&
        project.hasProperty("signing.password") &&
        project.hasProperty("signing.secretKeyRingFile")
val catalog: VersionCatalog = project.versionCatalogs.named("libs")

val remapJarTask: org.gradle.jvm.tasks.Jar by tasks.named("remapJar", org.gradle.jvm.tasks.Jar::class)

signing {
    sign(publishing.publications)
    sign(remapJarTask)
}

tasks {
    val jksSignJar = register("jksSignJar") {
        dependsOn(remapJar)
        onlyIf {
            project.hasProperty("jarSign.keyAlias") &&
                    project.hasProperty("jarSign.keyLocation") &&
                    project.hasProperty("jarSign.storePass")
        }
        doLast {
            ant.withGroovyBuilder {
                "signjar"(
                    "jar" to remapJar.flatMap { it.archiveFile }.get(),
                    "alias" to project.findProperty("jarSign.keyAlias"),
                    "keystore" to project.findProperty("jarSign.keyLocation"),
                    "storepass" to project.findProperty("jarSign.storePass"),
                    "sigalg" to "Ed25519",
                    "digestalg" to "SHA-256",
                    "tsaurl" to "http://timestamp.digicert.com",
                )
            }
        }
    }
    remapJar {
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
    file = remapJarTask.archiveFile
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
            start =minecraftVersion
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
