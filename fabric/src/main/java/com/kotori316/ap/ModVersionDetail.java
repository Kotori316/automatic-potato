package com.kotori316.ap;

import com.kotori316.ap.api.VersionStatusHolder;
import net.fabricmc.loader.api.Version;

import java.net.URI;
import java.util.function.Consumer;

record ModVersionDetail(
    String modId,
    Version modVersion,
    URI versionJsonUrl,
    String targetMinecraftVersion,
    String actualMinecraftVersion,
    String loaderVersion,
    String httpMethod,
    Consumer<VersionStatusHolder> consumer
) {
}
