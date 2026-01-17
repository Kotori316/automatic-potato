package com.kotori316.ap.internal;

import com.kotori316.ap.api.HttpReader;

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
    public HttpResponse read(URI uri, String method, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod(method);
        connection.setConnectTimeout(this.timeout);
        connection.setReadTimeout(this.timeout);
        headers.forEach(connection::setRequestProperty);
        connection.connect();

        return new HttpURLResponse(connection);
    }

    private static final class HttpURLResponse implements HttpResponse {
        private final HttpURLConnection connection;

        private HttpURLResponse(HttpURLConnection connection) {
            this.connection = connection;
        }

        @Override
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
        public String getContentType() {
            return this.connection.getContentType();
        }

        @Override
        public String getResponseMessage() throws IOException {
            return this.connection.getResponseMessage();
        }

        @Override
        public void close() {
            this.connection.disconnect();
        }
    }
}
