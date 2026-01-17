package com.kotori316.ap;

import com.kotori316.ap.api.VersionStatusHolder;
import net.fabricmc.loader.api.Version;

import java.net.URI;
import java.util.function.Consumer;

final class ModVersionDetail {
    private final String modId;
    private final Version modVersion;
    private final URI versionJsonUrl;
    private final String targetMinecraftVersion;
    private final String actualMinecraftVersion;
    private final String loaderVersion;
    private final Consumer<VersionStatusHolder> consumer;

    ModVersionDetail(String modId, Version modVersion, URI versionJsonUrl, String targetMinecraftVersion, String actualMinecraftVersion, String loaderVersion, Consumer<VersionStatusHolder> consumer) {
        this.modId = modId;
        this.modVersion = modVersion;
        this.versionJsonUrl = versionJsonUrl;
        this.targetMinecraftVersion = targetMinecraftVersion;
        this.actualMinecraftVersion = actualMinecraftVersion;
        this.loaderVersion = loaderVersion;
        this.consumer = consumer;
    }

    public String modId() {
        return modId;
    }

    public Version modVersion() {
        return modVersion;
    }

    public URI versionJsonUrl() {
        return versionJsonUrl;
    }

    public String targetMinecraftVersion() {
        return targetMinecraftVersion;
    }

    public String actualMinecraftVersion() {
        return actualMinecraftVersion;
    }

    public String loaderVersion() {
        return loaderVersion;
    }

    public Consumer<VersionStatusHolder> consumer() {
        return consumer;
    }
}
