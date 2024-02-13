plugins {
    id("com.kotori316.ap.common")
    id("com.kotori316.ap.publish")
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
}
