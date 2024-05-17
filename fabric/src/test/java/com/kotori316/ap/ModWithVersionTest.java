package com.kotori316.ap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kotori316.ap.api.VersionStatus;
import com.kotori316.ap.api.VersionStatusHolder;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ModWithVersionTest {
    ModWithVersion version;

    @BeforeEach
    void setup() throws VersionParsingException {
        this.version = new ModWithVersion(
            VersionCheckerMod.MOD_ID,
            Version.parse("1.0.0"),
            URI.create("https://version.kotori316.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            "1.16.5",
            "1.20.5",
            s -> {
            },
            "1.0"
        );
    }

    @Test
    void createInstance() {
        assertDoesNotThrow(() -> new ModWithVersion(
            VersionCheckerMod.MOD_ID,
            Version.parse("1.0.0"),
            URI.create("https://version.kotori316.com/get-version/1.16.5/fabric/" + VersionCheckerMod.MOD_ID),
            "1.16.5",
            "1.20.5",
            s -> {
            },
            "1.0"
        ));
    }

    @Test
    void userAgent() {
        String expected = String.format("%s/%s Java/%s Minecraft/%s Fabric/%s",
            VersionCheckerMod.MOD_ID,
            "1.0.0",
            System.getProperty("java.vendor.version"),
            "1.20.5",
            "1.0"
        );
        assertEquals(expected, this.version.getUa());
    }

    @Nested
    class CompareVersionTest {
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
            assertNotNull(this.response);
        }

        @Test
        void latest() throws VersionParsingException {
            AtomicReference<VersionStatusHolder> ref = new AtomicReference<>();
            ModWithVersion.compareVersion(this.response, "1.16.5", VersionCheckerMod.MOD_ID,
                Version.parse("2.2.0"), ref::set);

            VersionStatusHolder statusHolder = ref.get();
            assertNotNull(statusHolder, "Version must be set");
            assertAll(
                () -> assertEquals(VersionStatus.LATEST, statusHolder.versionStatus()),
                () -> assertEquals(VersionCheckerMod.MOD_ID, statusHolder.modId()),
                () -> assertEquals(Version.parse("2.2.0"), statusHolder.latestVersion()),
                () -> assertEquals(Version.parse("2.2.0"), statusHolder.currentVersion()),
                () -> assertEquals("https://modrinth.com/project/automatic-potato", statusHolder.homepage())
            );
        }

        @Test
        void ahead() throws VersionParsingException {
            AtomicReference<VersionStatusHolder> ref = new AtomicReference<>();
            ModWithVersion.compareVersion(this.response, "1.16.5", VersionCheckerMod.MOD_ID,
                Version.parse("2.3.0"), ref::set);

            VersionStatusHolder statusHolder = ref.get();
            assertNotNull(statusHolder, "Version must be set");
            assertEquals(VersionStatus.AHEAD, statusHolder.versionStatus());
            assertEquals(Version.parse("2.3.0"), statusHolder.currentVersion());
        }

        @Test
        void outdated() throws VersionParsingException {
            AtomicReference<VersionStatusHolder> ref = new AtomicReference<>();
            ModWithVersion.compareVersion(this.response, "1.16.5", VersionCheckerMod.MOD_ID,
                Version.parse("2.0.0"), ref::set);

            VersionStatusHolder statusHolder = ref.get();
            assertNotNull(statusHolder, "Version must be set");
            assertEquals(VersionStatus.OUTDATED, statusHolder.versionStatus());
            assertEquals(Version.parse("2.0.0"), statusHolder.currentVersion());
        }

        @Test
        void outdated2() throws VersionParsingException {
            AtomicReference<VersionStatusHolder> ref = new AtomicReference<>();
            ModWithVersion.compareVersion(this.response, "1.16.5", VersionCheckerMod.MOD_ID,
                Version.parse("1.0.0"), ref::set);

            VersionStatusHolder statusHolder = ref.get();
            assertNotNull(statusHolder, "Version must be set");
            assertEquals(VersionStatus.OUTDATED, statusHolder.versionStatus());
            assertEquals(Version.parse("1.0.0"), statusHolder.currentVersion());
        }

        @Test
        void notFound() throws VersionParsingException {
            AtomicReference<VersionStatusHolder> ref = new AtomicReference<>();
            ModWithVersion.compareVersion(this.response, "1.20.5", VersionCheckerMod.MOD_ID,
                Version.parse("2.0.0"), ref::set);
            assertNull(ref.get());
        }
    }

    @Nested
    class GetRecommendedVersion {
        JsonObject response;

        @BeforeEach
        void setup() throws IOException {
            try (InputStream stream = Objects.requireNonNull(
                ModWithVersionTest.class.getResourceAsStream("/response/1.19-recommended.json"),
                "Response reading stream is null"
            );
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream))
            ) {
                this.response = new Gson().fromJson(reader, JsonObject.class);
            }
            assertNotNull(this.response);
        }

        @Test
        void latest() throws VersionParsingException {
            AtomicReference<VersionStatusHolder> ref = new AtomicReference<>();
            ModWithVersion.compareVersion(this.response, "1.19", VersionCheckerMod.MOD_ID,
                Version.parse("19.0.1"), ref::set);

            VersionStatusHolder statusHolder = ref.get();
            assertNotNull(statusHolder, "Version must be set");
            assertEquals(VersionStatus.LATEST, statusHolder.versionStatus());
            assertEquals(Version.parse("19.0.1"), statusHolder.currentVersion());
        }

        @Test
        void ahead() throws VersionParsingException {
            AtomicReference<VersionStatusHolder> ref = new AtomicReference<>();
            ModWithVersion.compareVersion(this.response, "1.19", VersionCheckerMod.MOD_ID,
                Version.parse("19.1.0"), ref::set);

            VersionStatusHolder statusHolder = ref.get();
            assertNotNull(statusHolder, "Version must be set");
            assertEquals(VersionStatus.AHEAD, statusHolder.versionStatus());
            assertEquals(Version.parse("19.1.0"), statusHolder.currentVersion());
            assertEquals(Version.parse("19.0.1"), statusHolder.latestVersion());
        }

        @Test
        void useLatest() throws VersionParsingException, IOException {
            JsonObject response;
            try (InputStream stream = Objects.requireNonNull(
                ModWithVersionTest.class.getResourceAsStream("/response/1.19-latest.json"),
                "Response reading stream is null"
            );
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream))
            ) {
                response = new Gson().fromJson(reader, JsonObject.class);
            }
            AtomicReference<VersionStatusHolder> ref = new AtomicReference<>();
            ModWithVersion.compareVersion(response, "1.19", VersionCheckerMod.MOD_ID,
                Version.parse("19.1.1"), ref::set);

            VersionStatusHolder statusHolder = ref.get();
            assertNotNull(statusHolder, "Version must be set");
            assertEquals(VersionStatus.LATEST, statusHolder.versionStatus());
            assertEquals(Version.parse("19.1.1"), statusHolder.currentVersion());
        }
    }
}
