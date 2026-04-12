package com.kotori316.ap.internal;

import com.kotori316.ap.api.HttpReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

public final class HttpURLConnectionReader implements HttpReader {
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private final int timeout;

    @SuppressWarnings("unused") // called by ServiceLoader
    public HttpURLConnectionReader() {
        this(DEFAULT_TIMEOUT_MS);
    }

    public HttpURLConnectionReader(int timeout) {
        this.timeout = timeout;
    }

    @Override
    @NotNull
    public HttpResponse read(@NotNull URI uri, @NotNull String method, @NotNull Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod(method);
        connection.setConnectTimeout(this.timeout);
        connection.setReadTimeout(this.timeout);
        headers.forEach(connection::setRequestProperty);
        connection.connect();

        return new HttpURLResponse(connection);
    }

    private record HttpURLResponse(HttpURLConnection connection) implements HttpResponse {
        @Override
        @NotNull
        public InputStream getInputStream() throws IOException {
            return this.connection.getInputStream();
        }

        @Override
        public int getResponseCode() throws IOException {
            return this.connection.getResponseCode();
        }

        @Override
        public boolean isOk() throws IOException {
            return this.connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        }

        @Override
        @Nullable
        public String getContentType() {
            return this.connection.getContentType();
        }

        @Override
        public void close() {
            this.connection.disconnect();
        }
    }
}
