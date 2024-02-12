plugins {
    id("com.kotori316.ap.common")
    id("com.kotori316.ap.publish")
    // https://maven.fabricmc.net/net/fabricmc/fabric-loom/
    id("fabric-loom") version ("1.5.7")
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
}
