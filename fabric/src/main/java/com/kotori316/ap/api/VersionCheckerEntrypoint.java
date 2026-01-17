package com.kotori316.ap.api;

import com.kotori316.ap.VersionCheckerMod;

import java.net.URI;
import java.util.Optional;

public interface VersionCheckerEntrypoint {

    /**
     * @return the valid URL of version json
     */
    URI versionJsonUrl();

    /**
     * You can override this method to change the minecraft version to get mod version.
     * This value is used as JSON key, like {@code (target version)-latest} or {@code (target version)-recommended} to get the latest version.
     *
     * @return {@link Optional#empty()} to use current minecraft version. {@link Optional#of(Object)} to "fix" the version.
     */
    default Optional<String> targetMinecraftVersion() {
        return Optional.empty();
    }

    /**
     * @return the HTTP method to use to get version json. Default is {@code GET}
     */
    default String httpMethod() {
        return "GET";
    }

    /**
     * @return whether to enable version check of this mod
     */
    default boolean enabled() {
        return true;
    }

    /**
     * This method is called after version is compared. The default action is just logging out, but you can change by overriding this method.
     *
     * @param holder the result of comparison.
     */
    default void log(VersionStatusHolder holder) {
        logVersionInfo(holder);
    }

    /**
     * The default action, independent of actual minecraft version.
     */
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
