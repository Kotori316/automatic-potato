package com.kotori316.ap;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.kotori316.ap.api.VersionStatus;
import com.kotori316.ap.api.VersionStatusHolder;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

final class ModWithVersion {
    private final String modId;
    private final Version modVersion;
    private final URI versionJsonUrl;
    private final String minecraftVersion;
    private final String loaderVersion;
    private final Consumer<VersionStatusHolder> consumer;

    ModWithVersion(String modId, Version modVersion, URI versionJsonUrl, String minecraftVersion, String loaderVersion, Consumer<VersionStatusHolder> consumer) {
        this.modId = modId;
        this.modVersion = modVersion;
        this.versionJsonUrl = versionJsonUrl;
        this.minecraftVersion = minecraftVersion;
        this.loaderVersion = loaderVersion;
        this.consumer = consumer;
    }

    void check() {
        Gson gson = new Gson();

        String userAgent = String.format("%s Java/%s Minecraft/%s Fabric/%s", modId, System.getProperty("java.vendor.version"), minecraftVersion, loaderVersion);
        try {
            VersionCheckerMod.LOGGER.debug("Access to {} for {}({}) with UA '{}'", this.versionJsonUrl, this.modId, this.modVersion, userAgent);
            HttpURLConnection connection = (HttpURLConnection) this.versionJsonUrl.toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", userAgent);

            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                 JsonReader jsonReader = new JsonReader(reader)
            ) {
                if (connection.getResponseCode() >= 300) {
                    VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}. Message: {}", modId, connection.getResponseMessage());
                    return;
                }
                JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
                compareVersion(jsonObject, minecraftVersion, modId, modVersion, consumer);
            }
        } catch (IOException e) {
            VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}. Message: {}", modId, e.getMessage());
        }
    }

    /**
     * @see <a href="https://docs.minecraftforge.net/en/1.20.x/misc/updatechecker/">Forge Update Checker</a>
     */
    private static void compareVersion(JsonObject jsonObject, String minecraftVersion, String modId, Version modVersion, Consumer<VersionStatusHolder> consumer) {
        Optional<String> homepage = Optional.ofNullable(jsonObject.get("homepage")).map(JsonElement::getAsString);
        Version latestVersion = Optional.ofNullable(jsonObject.getAsJsonObject("promos"))
            .map(j -> j.get(minecraftVersion + "-latest"))
            .map(JsonElement::getAsString)
            .map(ModWithVersion::parseVersion)
            .orElse(null);
        if (latestVersion == null) {
            VersionCheckerMod.LOGGER.info("No version found for {}", modId);
            return;
        }
        int compare = modVersion.compareTo(latestVersion);
        VersionStatus status;
        if (compare > 0) {
            status = VersionStatus.AHEAD;
        } else if (compare == 0) {
            status = VersionStatus.LATEST;
        } else {
            status = VersionStatus.OUTDATED;
        }
        consumer.accept(new VersionStatusHolder(status, modId, homepage.orElse(null), latestVersion, modVersion));
    }

    Runnable asRunnable() {
        return this::check;
    }

    private static Version parseVersion(String version) throws IllegalArgumentException {
        try {
            return Version.parse(version);
        } catch (VersionParsingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
