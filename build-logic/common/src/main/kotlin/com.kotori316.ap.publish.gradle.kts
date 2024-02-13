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
val publishMcVersion = listOf(
    "1.16.5",
    "1.17.1",
    "1.18.2",
    "1.19.2",
    "1.19.4",
    "1.20.1",
    "1.20.2",
    "1.20.4",
)
val hasGpgSignature = project.hasProperty("signing.keyId") &&
        project.hasProperty("signing.password") &&
        project.hasProperty("signing.secretKeyRingFile")

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
        platformVersion = "0.15.+"
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
        minecraftVersions = publishMcVersion
    }

    modrinth {
        accessToken = (project.findProperty("modrinthToken") ?: System.getenv("MODRINTH_TOKEN") ?: "") as String
        projectId = "fLArsyMM"
        minecraftVersions = publishMcVersion
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
        if (System.getenv("CLOUDFLARE_S3_ENDPOINT") != null || !releaseDebug) {
            val r2AccessKey = (project.findProperty("r2_access_key") ?: System.getenv("R2_ACCESS_KEY") ?: "") as String
            val r2SecretKey = (project.findProperty("r2_secret_key") ?: System.getenv("R2_SECRET_KEY") ?: "") as String
            maven {
                name = "kotori316-maven"
                url = uri("s3://kotori316-maven")
                credentials(AwsCredentials::class) {
                    accessKey = r2AccessKey
                    secretKey = r2SecretKey
                }
            }
        }
    }
}
