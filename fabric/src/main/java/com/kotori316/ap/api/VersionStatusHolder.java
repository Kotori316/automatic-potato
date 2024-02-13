package com.kotori316.ap.api;

import net.fabricmc.loader.api.Version;
import org.jetbrains.annotations.Nullable;

public final class VersionStatusHolder {
    private final VersionStatus versionStatus;
    private final String modId;
    @Nullable
    private final String homepage;
    private final Version latestVersion;
    private final Version currentVersion;

    public VersionStatusHolder(VersionStatus versionStatus, String modId, @Nullable String homepage, Version latestVersion, Version currentVersion) {
        this.versionStatus = versionStatus;
        this.modId = modId;
        this.homepage = homepage;
        this.latestVersion = latestVersion;
        this.currentVersion = currentVersion;
    }

    public VersionStatus versionStatus() {
        return versionStatus;
    }

    public String modId() {
        return modId;
    }

    public @Nullable String homepage() {
        return homepage;
    }

    public Version latestVersion() {
        return latestVersion;
    }

    public Version currentVersion() {
        return currentVersion;
    }
}
