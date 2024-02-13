package com.kotori316.ap;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.kotori316.ap.api.VersionStatus;
import com.kotori316.ap.api.VersionStatusHolder;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.apache.http.HttpVersion;
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
        RequestConfig config = RequestConfig.custom()
            .setSocketTimeout(5000)
            .setConnectTimeout(5000)
            .setMaxRedirects(5)
            .build();
        Gson gson = new Gson();

        String userAgent = String.format("%s Java/%s Minecraft/%s Fabric/%s", modId, System.getProperty("java.vendor.version"), minecraftVersion, loaderVersion);
        try (CloseableHttpClient client = HttpClientBuilder.create().setUserAgent(userAgent).setDefaultRequestConfig(config).build()) {
            HttpGet get = new HttpGet(this.versionJsonUrl);
            get.setProtocolVersion(HttpVersion.HTTP_1_1);
            VersionCheckerMod.LOGGER.debug("Access to {} with UA '{}'", get, userAgent);
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
                compareVersion(jsonObject, minecraftVersion, modId, modVersion, consumer);
            }
        } catch (Exception e) {
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
