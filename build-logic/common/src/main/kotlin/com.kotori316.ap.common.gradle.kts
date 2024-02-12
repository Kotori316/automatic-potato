plugins {
    java
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}
