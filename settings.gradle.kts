pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.minecraftforge.net/") }
        maven { url = uri("https://maven.neoforged.net/releases") }
        maven { url = uri("https://maven.fabricmc.net") }
        maven { url = uri("https://maven.parchmentmc.org") }
        maven { url = uri("https://maven.kotori316.com") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
    id("com.gradle.develocity") version ("4.+")
}

develocity {
    buildScan {
        if (System.getenv("CI").toBoolean()) {
            termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
            termsOfUseAgree = "yes"
        }
        publishing {
            onlyIf { false }
        }
    }
}

includeBuild("build-logic")
rootProject.name = "automatic-potato"
if (!System.getenv("DISABLE_FORGE").toBoolean()) {
    // include("forge")
}
if (!System.getenv("DISABLE_FABRIC").toBoolean()) {
    include("fabric")
}
if (!System.getenv("DISABLE_NEOFORGE").toBoolean()) {
    // include("neoforge")
}
