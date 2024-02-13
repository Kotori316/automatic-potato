package com.kotori316.ap;

import java.net.URI;
import java.util.Optional;

public final class VersionCheckerEntrypointImpl implements VersionCheckerEntrypoint {

    @Override
    public URI versionJsonUrl() {
        return URI.create(String.format("https://version.kotori316.com/get-version/%s/%s/%s", "1.16.5", "fabric", VersionCheckerMod.MOD_ID));
    }

    @Override
    public Optional<String> targetMinecraftVersion() {
        return Optional.of("1.16.5");
    }

    @Override
    public boolean enabled() {
        return false;
    }
}
