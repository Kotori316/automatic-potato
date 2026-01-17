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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

final class ModWithVersion {
    private final ModVersionDetail detail;
    private final HttpReader httpReader;

    ModWithVersion(ModVersionDetail detail, HttpReader httpReader) {
        this.detail = detail;
        this.httpReader = httpReader;
    }

    CheckConnectionStatus check() {
        try {
            String userAgent = this.getUa();
            VersionCheckerMod.LOGGER.debug("Access to {} for {}({}) with UA '{}'", this.detail.versionJsonUrl(), this.detail.modId(), this.detail.modVersion(), userAgent);
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", userAgent);
            headers.put("Accept", "application/json");

            try (HttpReader.HttpResponse response = this.httpReader.read(this.detail.versionJsonUrl(), "GET", Collections.unmodifiableMap(headers))) {
                String contentType = response.getContentType();
                String responseMessage = response.getResponseMessage();

                if (response.isOk()) {
                    if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
                        try (InputStream inputStream = response.getInputStream();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                             JsonReader jsonReader = new JsonReader(reader)
                        ) {
                            Gson gson = new Gson();
                            JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
                            VersionCheckerMod.LOGGER.debug("Get json for {}/{}: {}", this.detail.modId(), this.detail.modVersion(), jsonObject);
                            compareVersion(jsonObject, this.detail.targetMinecraftVersion(), this.detail.modId(), this.detail.modVersion(), this.detail.consumer());
                            return CheckConnectionStatus.OK;
                        }
                    } else {
                        VersionCheckerMod.LOGGER.warn("Expected application/json, but got {} for {}/{}", contentType, this.detail.modId(), this.detail.modVersion());
                        return CheckConnectionStatus.INVALID_CONTENT_TYPE;
                    }
                } else {
                    VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}/{}. Message: {}, Status: {}", this.detail.modId(), this.detail.modVersion(), responseMessage, response.getResponseCode());
                    return CheckConnectionStatus.INVALID_STATUS_CODE;
                }
            }
        } catch (IOException | JsonParseException e) {
            VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}/{}. Message: {}", this.detail.modId(), this.detail.modVersion(), e.getMessage());
            VersionCheckerMod.LOGGER.debug("Stacktrace of {}({})", this.detail.modId(), this.detail.modVersion(), e);
            return CheckConnectionStatus.ERROR;
        }
    }

    String getUa() {
        return String.format("%s/%s Java/%s Minecraft/%s Fabric/%s", this.detail.modId(), this.detail.modVersion(), System.getProperty("java.vendor.version"), this.detail.actualMinecraftVersion(), this.detail.loaderVersion());
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
