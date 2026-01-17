package com.kotori316.ap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kotori316.ap.internal.HttpURLConnectionReader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

final class ModWithVersionConnectionTest {
    MockWebServer server;
    JsonObject response;

    @BeforeEach
    void setup() throws IOException {
        this.server = new MockWebServer();
        try (InputStream stream = Objects.requireNonNull(
            ModWithVersionTest.class.getResourceAsStream("/response/2.2.0.json"),
            "Response reading stream is null"
        );
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))
        ) {
            this.response = new Gson().fromJson(reader, JsonObject.class);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        this.server.shutdown();
    }

    private ModWithVersion createVersion(HttpUrl url) {
        return createVersion(url, 5000);
    }

    private ModWithVersion createVersion(HttpUrl url, int timeout) {
        try {
            return new ModWithVersion(
                new ModVersionDetail(
                    VersionCheckerMod.MOD_ID,
                    Version.parse("1.0.0"),
                    url.uri(),
                    "1.16.5",
                    "1.20.5",
                    "1.0",
                    s -> {
                    }
                ),
                new HttpURLConnectionReader(timeout)
            );
        } catch (VersionParsingException e) {
            fail(e);
            throw new AssertionError("Unreachable");
        }
    }

    @Test
    void success() throws IOException {
        server.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(new Gson().toJson(response))
        );
        server.start();
        ModWithVersion version = createVersion(server.url("/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID));
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.OK, status);
    }

    @Test
    void notFound() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(404));
        server.start();
        ModWithVersion version = createVersion(server.url("/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID));
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_STATUS_CODE, status);
    }

    @Test
    void internalServerError() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(500).addHeader("Content-Type", "text/html"));
        server.start();
        ModWithVersion version = createVersion(server.url("/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID));
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_STATUS_CODE, status);
    }

    @Test
    void returnsHtml() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "text/html"));
        server.start();
        ModWithVersion version = createVersion(server.url("/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID));
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_CONTENT_TYPE, status);
    }

    @Test
    void returnsText() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "text/plain"));
        server.start();
        ModWithVersion version = createVersion(server.url("/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID));
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_CONTENT_TYPE, status);
    }

    @Test
    void connectionErrorBodyDelay() throws IOException {
        server.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(new Gson().toJson(response))
                .setBodyDelay(100, TimeUnit.MILLISECONDS)
        );
        server.start();
        ModWithVersion version = createVersion(server.url("/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID), 50);
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.ERROR, status);
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 400, 500})
    void connectionErrorBodyDelay2(int statusCode) throws IOException {
        server.enqueue(
            new MockResponse()
                .setResponseCode(statusCode)
                .addHeader("Content-Type", "application/json")
                .setBody(new Gson().toJson(response))
                .setBodyDelay(100, TimeUnit.MILLISECONDS)
        );
        server.start();
        ModWithVersion version = createVersion(server.url("/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID), 50);
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_STATUS_CODE, status);

    }

    @ParameterizedTest
    @ValueSource(ints = {200, 300, 400, 500})
    void connectionErrorHeaderDelay(int responseCode) throws IOException {
        server.enqueue(
            new MockResponse()
                .setResponseCode(responseCode)
                .addHeader("Content-Type", "text/plain")
                .setBody("Header Delay")
                .setHeadersDelay(100, TimeUnit.MILLISECONDS)
        );
        server.start();
        ModWithVersion version = createVersion(server.url("/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID), 50);
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.ERROR, status);
    }
}
