package com.kotori316.ap;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
    private final String targetMinecraftVersion;
    private final String actualMinecraftVersion;
    private final String loaderVersion;
    private final Consumer<VersionStatusHolder> consumer;

    ModWithVersion(String modId, Version modVersion, URI versionJsonUrl, String targetMinecraftVersion, String actualMinecraftVersion, Consumer<VersionStatusHolder> consumer, String loaderVersion) {
        this.modId = modId;
        this.modVersion = modVersion;
        this.versionJsonUrl = versionJsonUrl;
        this.targetMinecraftVersion = targetMinecraftVersion;
        this.actualMinecraftVersion = actualMinecraftVersion;
        this.loaderVersion = loaderVersion;
        this.consumer = consumer;
    }

    CheckConnectionStatus check() {
        return check(5000);
    }

    CheckConnectionStatus check(int timeout) {
        try {
            String userAgent = this.getUa();
            VersionCheckerMod.LOGGER.debug("Access to {} for {}({}) with UA '{}'", this.versionJsonUrl, this.modId, this.modVersion, userAgent);
            HttpURLConnection connection = (HttpURLConnection) this.versionJsonUrl.toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Accept", "application/json");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String contentType = connection.getContentType();
                if (contentType.startsWith("application/json")) {
                    try (InputStream inputStream = connection.getInputStream();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                         JsonReader jsonReader = new JsonReader(reader)
                    ) {
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
                        VersionCheckerMod.LOGGER.debug("Get json for {}/{}: {}", this.modId, this.modVersion, jsonObject);
                        compareVersion(jsonObject, targetMinecraftVersion, modId, modVersion, consumer);
                        return CheckConnectionStatus.OK;
                    }
                } else {
                    VersionCheckerMod.LOGGER.warn("Expected application/json, but got {} for {}/{}", contentType, modId, modVersion);
                    return CheckConnectionStatus.INVALID_CONTENT_TYPE;
                }
            } else {
                VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}/{}. Message: {}, Status: {}", modId, modVersion, connection.getResponseMessage(), connection.getResponseCode());
                return CheckConnectionStatus.INVALID_STATUS_CODE;
            }
        } catch (IOException | JsonParseException e) {
            VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}/{}. Message: {}", modId, modVersion, e.getMessage());
            VersionCheckerMod.LOGGER.debug("Stacktrace of {}({})", this.modId, this.modVersion, e);
            return CheckConnectionStatus.ERROR;
        }
    }

    String getUa() {
        return String.format("%s/%s Java/%s Minecraft/%s Fabric/%s", modId, modVersion, System.getProperty("java.vendor.version"), actualMinecraftVersion, loaderVersion);
    }

    /**
     * @see <a href="https://docs.minecraftforge.net/en/1.20.x/misc/updatechecker/">Forge Update Checker</a>
     */
    static void compareVersion(JsonObject jsonObject, String minecraftVersion, String modId, Version modVersion, Consumer<VersionStatusHolder> consumer) {
        Optional<String> homepage = Optional.ofNullable(jsonObject.get("homepage")).map(JsonElement::getAsString);
        Version latestVersion = Optional.ofNullable(jsonObject.getAsJsonObject("promos"))
            .map(j ->
                Optional.ofNullable(j.get(minecraftVersion + "-latest"))
                    .orElseGet(() -> j.get(minecraftVersion + "-recommended"))
            )
            .map(JsonElement::getAsString)
            .map(ModWithVersion::parseVersion)
            .orElse(null);
        if (latestVersion == null) {
            VersionCheckerMod.LOGGER.info("No version found for {}({}) for Minecraft {}", modId, modVersion, minecraftVersion);
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
