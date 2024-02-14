# automatic-potato

Mod version checker for Fabric mods

# Download

* https://www.curseforge.com/minecraft/mc-mods/automatic-potato
* https://modrinth.com/mod/automatic-potato

# For developer

This mod checks the mod version with JSON compatible with [Forge Update Checker].
See Forge document for detail.

[Forge Update Checker]: https://docs.minecraftforge.net/en/1.20.x/misc/updatechecker/

This mod supports homepage and promos.<mcversion>-latest fields.
Other entries including the changelog are not loaded currently.
The mod version and latest version will be compared, then the result will be logged in console.

## Simple implementation

After preparing the JSON, set the URL in "custom" in `fabric.mod.json`.

```json
{
  "custom": {
    "kotori316_version_checker": "https://version.kotori316.com/get-version/1.16.5/fabric/kotori316_version_checker"
  }
}
```

A complete example can be found in this repo.
https://github.com/Kotori316/automatic-potato/blob/main/example/fabric.mod.json

## Dynamic implementation

You can change the URL and the action after comparing(default is just logging).

Include this mod as dependency like this.

```groovy
repositories {
    maven { url = uri("https://maven.kotori316.com") }
}
dependencies {
    // See https://maven.kotori316.com/com/kotori316/VersionCheckerMod for latest version
    modImplementation("com.kotori316:VersionCheckerMod:2.0.0") {
        transitive(false)
    }
}
```

Then, create a class implementing `com.kotori316.ap.api.VersionCheckerEntrypoint`, and set the class in entrypoints in
your `fabric.mod.json`.

```json
{
  "entrypoints": {
    "main": [
      "your_mod_entrypoint"
    ],
    "kotori316_version_checker": [
      "com.kotori316.ap.VersionCheckerEntrypointImpl"
    ]
  }
}
```

See https://github.com/Kotori316/automatic-potato/blob/main/fabric/src/main/java/com/kotori316/ap/VersionCheckerEntrypointImpl.java
for an example to implement the entry point.
