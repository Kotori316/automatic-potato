package com.kotori316.ap.internal;

import com.kotori316.ap.api.HttpReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

public final class HttpClientReader implements HttpReader {
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private final Duration timeout;

    @SuppressWarnings("unused") // called by ServiceLoader
    public HttpClientReader() {
        this(DEFAULT_TIMEOUT_MS);
    }

    public HttpClientReader(int timeoutMs) {
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public HttpResponse read(URI uri, String method, Map<String, String> headers) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(this.timeout)
            .method(method, HttpRequest.BodyPublishers.noBody());
        headers.forEach(requestBuilder::header);

        try (HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(this.timeout)
            .build()) {
            return new HttpClientResponse(client.send(
                requestBuilder.build(),
                java.net.http.HttpResponse.BodyHandlers.ofInputStream()
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }

    @Override
    public int priority() {
        return 10;
    }

    private record HttpClientResponse(java.net.http.HttpResponse<InputStream> response) implements HttpResponse {
        @Override
        public InputStream getInputStream() {
            return this.response.body();
        }

        @Override
        public int getResponseCode() {
            return this.response.statusCode();
        }

        @Override
        public boolean isOk() {
            return this.response.statusCode() == 200;
        }

        @Override
        public String getContentType() {
            return this.response.headers().firstValue("content-type").orElse(null);
        }

        @Override
        public void close() throws IOException {
            this.response.body().close();
        }
    }
}
