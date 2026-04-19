plugins {
    id("com.kotori316.ap.common")
    id("com.kotori316.ap.publish")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraftVersion")}")

    implementation(libs.fabric.loader)
}
