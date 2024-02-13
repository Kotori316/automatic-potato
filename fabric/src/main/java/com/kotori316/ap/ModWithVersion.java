package com.kotori316.ap;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Optional;

final class ModWithVersion {
    private final String modId;
    private final Version modVersion;
    private final URI versionJsonUrl;
    private final String minecraftVersion;
    private final String loaderVersion;

    ModWithVersion(String modId, Version modVersion, URI versionJsonUrl, String minecraftVersion, String loaderVersion) {
        this.modId = modId;
        this.modVersion = modVersion;
        this.versionJsonUrl = versionJsonUrl;
        this.minecraftVersion = minecraftVersion;
        this.loaderVersion = loaderVersion;
    }

    void check() {
        RequestConfig config = RequestConfig.custom()
            .setSocketTimeout(5000)
            .setConnectTimeout(5000)
            .setMaxRedirects(5)
            .build();
        Gson gson = new Gson();

        try (CloseableHttpClient client = HttpClientBuilder.create().setUserAgent(
            String.format("%s Java/%s Minecraft/%s Fabric/%s", modId, System.getProperty("java.vendor.version"), minecraftVersion, loaderVersion)
        ).setDefaultRequestConfig(config).build()) {
            HttpGet get = new HttpGet(this.versionJsonUrl);
            try (CloseableHttpResponse response = client.execute(get);
                 BufferedInputStream stream = new BufferedInputStream(response.getEntity().getContent());
                 Reader reader = new InputStreamReader(stream);
                 JsonReader jsonReader = new JsonReader(reader)
            ) {
                if (response.getStatusLine().getStatusCode() >= 300) {
                    VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}. Message: {}", modId, response.getStatusLine().getReasonPhrase());
                    return;
                }
                JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
                compareVersion(jsonObject, minecraftVersion, modId, modVersion);
            }
        } catch (Exception e) {
            VersionCheckerMod.LOGGER.warn("Failed to get version JSON for {}. Message: {}", modId, e.getMessage());
        }
    }

    /**
     * @see <a href="https://docs.minecraftforge.net/en/1.20.x/misc/updatechecker/">Forge Update Checker</a>
     */
    private static void compareVersion(JsonObject jsonObject, String minecraftVersion, String modId, Version modVersion) {
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
        if (compare > 0) {
            // Ahead
            VersionCheckerMod.LOGGER.info("Using ahead version '{}' for {}", modVersion, modId);
        } else if (compare == 0) {
            // Latest
            VersionCheckerMod.LOGGER.info("Using the latest version, '{}' for {}", modVersion, modId);
        } else {
            // Outdated
            VersionCheckerMod.LOGGER.info("Using outdated version for {}. Latest: '{}', Current: '{}', Homepage: {}",
                modId, latestVersion.getFriendlyString(), modVersion, homepage.orElse("not present"));
        }
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
