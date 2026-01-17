package com.kotori316.ap;

import com.kotori316.ap.api.HttpReader;
import com.kotori316.ap.api.VersionCheckerEntrypoint;
import com.kotori316.ap.internal.HttpURLConnectionReader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class VersionCheckerMod implements ModInitializer {
    public static final String MOD_ID = "kotori316_version_checker";
    public static final String KEY = MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger("ForgeLikeVersionChecker");

    @Override
    public void onInitialize() {
        HttpReader reader = new HttpURLConnectionReader(5000);
        String loaderVersion = FabricLoader.getInstance().getModContainer("fabricloader").map(ModContainer::getMetadata).map(ModMetadata::getVersion).map(Version::getFriendlyString).orElse("none");
        String minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft").map(ModContainer::getMetadata).map(ModMetadata::getVersion).map(Version::getFriendlyString).orElse("none");
        List<EntrypointContainer<VersionCheckerEntrypoint>> list = FabricLoader.getInstance().getEntrypointContainers(KEY, VersionCheckerEntrypoint.class);
        Stream<ModWithVersion> fromEntryPoint = list.stream()
            .filter(e -> e.getEntrypoint().enabled())
            .map(e -> new ModWithVersion(
                e.getProvider().getMetadata().getId(),
                e.getProvider().getMetadata().getVersion(),
                e.getEntrypoint().versionJsonUrl(),
                e.getEntrypoint().targetMinecraftVersion().orElse(minecraftVersion),
                minecraftVersion,
                e.getEntrypoint()::log,
                loaderVersion,
                reader
            ));
        Stream<ModWithVersion> fromCustom = FabricLoader.getInstance()
            .getAllMods()
            .stream()
            .map(ModContainer::getMetadata)
            .filter(m -> m.containsCustomValue(KEY))
            .flatMap(m -> {
                try {
                    String uri = m.getCustomValue(KEY).getAsString();
                    return Stream.of(new ModWithVersion(
                        m.getId(),
                        m.getVersion(),
                        URI.create(uri),
                        minecraftVersion,
                        minecraftVersion,
                        VersionCheckerEntrypoint::logVersionInfo,
                        loaderVersion,
                        reader));
                } catch (RuntimeException e) {
                    LOGGER.warn("Invalid configuration of {} in {}", KEY, m.getId());
                    return Stream.empty();
                }
            });

        CompletableFuture<?>[] versions = Stream.concat(fromEntryPoint, fromCustom)
            .map(ModWithVersion::asRunnable)
            .map(CompletableFuture::runAsync)
            .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(versions);
    }

}
