package com.kotori316.ap;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.kotori316.ap.api.HttpReader;
import com.kotori316.ap.api.VersionStatus;
import com.kotori316.ap.api.VersionStatusHolder;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private final HttpReader httpReader;

    ModWithVersion(String modId, Version modVersion, URI versionJsonUrl, String targetMinecraftVersion, String actualMinecraftVersion, Consumer<VersionStatusHolder> consumer, String loaderVersion, HttpReader httpReader) {
        this.modId = modId;
        this.modVersion = modVersion;
        this.versionJsonUrl = versionJsonUrl;
        this.targetMinecraftVersion = targetMinecraftVersion;
        this.actualMinecraftVersion = actualMinecraftVersion;
        this.loaderVersion = loaderVersion;
        this.consumer = consumer;
        this.httpReader = httpReader;
    }

    CheckConnectionStatus check() {
        try {
            String userAgent = this.getUa();
            VersionCheckerMod.LOGGER.debug("Access to {} for {}({}) with UA '{}'", this.versionJsonUrl, this.modId, this.modVersion, userAgent);
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", userAgent);
            headers.put("Accept", "application/json");

            try (HttpReader.HttpResponse response = this.httpReader.read(this.versionJsonUrl, "GET", Collections.unmodifiableMap(headers))) {
                int responseCode = response.getResponseCode();
                String contentType = response.getContentType();
                String responseMessage = response.getResponseMessage();

                if (responseCode == 200) { // HttpURLConnection.HTTP_OK
                    if (contentType != null && contentType.startsWith("application/json")) {
                        try (InputStream inputStream = response.getInputStream();
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
                    VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}/{}. Message: {}, Status: {}", modId, modVersion, responseMessage, responseCode);
                    return CheckConnectionStatus.INVALID_STATUS_CODE;
                }
            }
        } catch (IOException | JsonParseException e) {
            VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}/{}. Message: {}", modId, modVersion, e.getMessage());
            VersionCheckerMod.LOGGER.debug("Stacktrace of {}({})", this.modId, this.modVersion, e);
            return CheckConnectionStatus.ERROR;
        }
    }

    @Deprecated
    CheckConnectionStatus check(int timeout) {
        return check();
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
