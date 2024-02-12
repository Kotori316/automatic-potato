import com.kotori316.plugin.cf.CallVersionCheckFunctionTask
import com.kotori316.plugin.cf.CallVersionFunctionTask

plugins {
    id("maven-publish")
    id("signing")
    id("me.modmuss50.mod-publish-plugin")
    id("com.kotori316.plugin.cf")
}

val modId: String by project
// Fixed. Use 1.16.5
val minecraftVersion: String by project
val releaseDebug = (System.getenv("RELEASE_DEBUG") ?: "true").toBoolean()
val publishMcVersion = listOf(
    "1.16.5",
    "1.17.2",
    "1.18.2",
    "1.19.2",
    "1.20.1",
    "1.20.4",
)
val hasGpgSignature = project.hasProperty("signing.keyId") &&
        project.hasProperty("signing.password") &&
        project.hasProperty("signing.secretKeyRingFile")

afterEvaluate {
    val remapJar: org.gradle.jvm.tasks.Jar by tasks.named("remapJar", org.gradle.jvm.tasks.Jar::class)
    signing {
        sign(publishing.publications)
        sign(remapJar)
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
                        "jar" to remapJar.archiveFile,
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
        remapJar.finalizedBy(jksSignJar)
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
            platformVersion = "0.15.+"
            modName = modId
            changelog = "Version ${project.version}"
            homepage = "https://github.com/Kotori316/automatic-potato"
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
        file = remapJar.archiveFile
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
            minecraftVersions = publishMcVersion
        }

        modrinth {
            accessToken = (project.findProperty("modrinthToken") ?: System.getenv("MODRINTH_TOKEN") ?: "") as String
            projectId = "fLArsyMM"
            minecraftVersions = publishMcVersion
        }
    }
}
