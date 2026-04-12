plugins {
    id("com.kotori316.ap.common")
    id("com.kotori316.ap.publish")
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
}
