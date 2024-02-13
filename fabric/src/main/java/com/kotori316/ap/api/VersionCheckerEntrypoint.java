package com.kotori316.ap.api;

import com.kotori316.ap.VersionCheckerMod;

import java.net.URI;
import java.util.Optional;

public interface VersionCheckerEntrypoint {
    URI versionJsonUrl();

    default Optional<String> targetMinecraftVersion() {
        return Optional.empty();
    }

    default boolean enabled() {
        return true;
    }

    default void log(VersionStatusHolder holder) {
        logVersionInfo(holder);
    }

    static void logVersionInfo(VersionStatusHolder holder) {
        switch (holder.versionStatus()) {
            case LATEST:
                VersionCheckerMod.LOGGER.info("Using the latest version, '{}' for {}", holder.currentVersion(), holder.modId());
                break;
            case AHEAD:
                VersionCheckerMod.LOGGER.info("Using ahead version '{}' for {}", holder.currentVersion(), holder.modId());
                break;
            case OUTDATED:
                VersionCheckerMod.LOGGER.info("Using outdated version for {}. Latest: '{}', Current: '{}', Homepage: {}",
                    holder.modId(), holder.latestVersion().getFriendlyString(), holder.currentVersion(), holder.homepage());
                break;
        }
    }
}
