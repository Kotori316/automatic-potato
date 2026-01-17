package com.kotori316.ap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kotori316.ap.api.HttpReader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

final class ModWithVersionConnectionTest {
    JsonObject response;

    @BeforeEach
    void setup() throws IOException {
        try (InputStream stream = Objects.requireNonNull(
            ModWithVersionTest.class.getResourceAsStream("/response/2.2.0.json"),
            "Response reading stream is null"
        );
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))
        ) {
            this.response = new Gson().fromJson(reader, JsonObject.class);
        }
    }

    private ModWithVersion createVersion(URI uri, Function<URI, HttpReader.HttpResponse> generator) {
        try {
            return new ModWithVersion(
                new ModVersionDetail(
                    VersionCheckerMod.MOD_ID,
                    Version.parse("1.0.0"),
                    uri,
                    "1.16.5",
                    "1.20.5",
                    "1.0",
                    "GET",
                    s -> {
                    }
                ),
                new FakeHttpReader(generator)
            );
        } catch (VersionParsingException e) {
            fail(e);
            throw new AssertionError("Unreachable");
        }
    }

    @Test
    void success() {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> new FakeHttpReader.FakeHttpResponse(200, "application/json", new Gson().toJson(response), "OK")
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.OK, status);
    }

    @Test
    void notFound() {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> new FakeHttpReader.FakeHttpResponse(404, "text/plain", "Not Found", "Not Found")
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_STATUS_CODE, status);
    }

    @Test
    void internalServerError() {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> new FakeHttpReader.FakeHttpResponse(500, "text/html", "<html>Error</html>", "Internal Server Error")
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_STATUS_CODE, status);
    }

    @Test
    void returnsHtml() {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> new FakeHttpReader.FakeHttpResponse(200, "text/html", "<html></html>", "OK")
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_CONTENT_TYPE, status);
    }

    @Test
    void returnsText() {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> new FakeHttpReader.FakeHttpResponse(200, "text/plain", "OK", "OK")
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_CONTENT_TYPE, status);
    }

    @Test
    void connectionErrorBodyDelay() {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> new FakeHttpReader.FakeHttpResponse(200, "application/json", new Gson().toJson(response), "OK", false, true)
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.ERROR, status);
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 400, 500})
    void connectionErrorBodyDelay2(int statusCode) {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> new FakeHttpReader.FakeHttpResponse(statusCode, "application/json", new Gson().toJson(response), "Error")
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.INVALID_STATUS_CODE, status);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 300, 400, 500})
    void connectionErrorHeaderDelay(int responseCode) {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> new FakeHttpReader.FakeHttpResponse(responseCode, "application/json", new Gson().toJson(response), "OK", true, false)
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.ERROR, status);
    }

    @Test
    void connectionErrorReadDelay() {
        ModWithVersion version = createVersion(
            URI.create("https://example.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            uri -> {
                throw new RuntimeException(new IOException("Connection Timeout"));
            }
        );
        CheckConnectionStatus status = version.check();
        assertEquals(CheckConnectionStatus.ERROR, status);
    }

    @Test
    void customMethod() throws Exception {
        AtomicReference<String> methodRef = new AtomicReference<>();
        ModWithVersion version = new ModWithVersion(
            new ModVersionDetail(
                VersionCheckerMod.MOD_ID,
                Version.parse("1.0.0"),
                URI.create("https://example.com"),
                "1.16.5",
                "1.20.5",
                "1.0",
                "POST",
                s -> {
                }
            ),
            new FakeHttpReader((uri, method) -> {
                methodRef.set(method);
                return new FakeHttpReader.FakeHttpResponse(200, "application/json", new Gson().toJson(response), "OK");
            })
        );
        version.check();
        assertEquals("POST", methodRef.get());
    }
}
