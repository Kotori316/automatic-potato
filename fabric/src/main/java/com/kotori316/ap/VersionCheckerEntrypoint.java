package com.kotori316.ap;

import java.net.URI;
import java.util.Optional;

public interface VersionCheckerEntrypoint {
    URI versionJsonUrl();

    default Optional<String> targetMinecraftVersion() {
        return Optional.empty();
    }
}
