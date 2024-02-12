package com.kotori316.ap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class VersionCheckerMod implements ModInitializer {
    public static final String MOD_ID = "kotori316_version_checker";
    public static final String KEY = MOD_ID;
    static final Logger LOGGER = LogManager.getLogger("ForgeLikeVersionChecker");

    @Override
    public void onInitialize() {
        List<EntrypointContainer<VersionCheckerEntrypoint>> list = FabricLoader.getInstance().getEntrypointContainers(KEY, VersionCheckerEntrypoint.class);
        String loaderVersion = FabricLoader.getInstance().getModContainer("fabricloader").map(ModContainer::getMetadata).map(ModMetadata::getVersion).map(Version::getFriendlyString).orElse("none");
        String minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft").map(ModContainer::getMetadata).map(ModMetadata::getVersion).map(Version::getFriendlyString).orElse("none");

        CompletableFuture<?>[] versions = list.stream()
            .map(e -> new ModWithVersion(
                e.getProvider().getMetadata().getId(),
                e.getProvider().getMetadata().getVersion(),
                e.getEntrypoint().versionJsonUrl(),
                e.getEntrypoint().targetMinecraftVersion().orElse(minecraftVersion),
                loaderVersion
            ))
            .map(ModWithVersion::asRunnable)
            .map(CompletableFuture::runAsync)
            .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(versions);
    }

}
