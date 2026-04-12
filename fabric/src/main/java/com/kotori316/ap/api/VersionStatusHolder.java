package com.kotori316.ap.api;

import net.fabricmc.loader.api.Version;
import org.jetbrains.annotations.Nullable;

public record VersionStatusHolder(
    VersionStatus versionStatus,
    String modId,
    @Nullable String homepage,
    Version latestVersion,
    Version currentVersion
) {
}
