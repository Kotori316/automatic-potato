package com.kotori316.ap;

import com.kotori316.ap.api.HttpReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;

final class FakeHttpReader implements HttpReader {
    private final Function<URI, HttpResponse> responseGenerator;

    FakeHttpReader(Function<URI, HttpResponse> responseGenerator) {
        this.responseGenerator = responseGenerator;
    }

    @Override
    public HttpResponse read(URI uri, String method, Map<String, String> headers) throws IOException {
        try {
            return responseGenerator.apply(uri);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    static final class FakeHttpResponse implements HttpResponse {
        private final int responseCode;
        private final String contentType;
        private final byte[] body;
        private final String message;
        private final boolean errorInHeader;
        private final boolean errorInBody;

        FakeHttpResponse(int responseCode, String contentType, String body, String message) {
            this(responseCode, contentType, body, message, false, false);
        }

        FakeHttpResponse(int responseCode, String contentType, String body, String message, boolean errorInHeader, boolean errorInBody) {
            this.responseCode = responseCode;
            this.contentType = contentType;
            this.body = body.getBytes();
            this.message = message;
            this.errorInHeader = errorInHeader;
            this.errorInBody = errorInBody;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (errorInBody) {
                throw new IOException("Connection Timeout in Body");
            }
            if (responseCode >= 400) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            return new ByteArrayInputStream(body);
        }

        @Override
        public int getResponseCode() throws IOException {
            if (errorInHeader) {
                throw new IOException("Connection Timeout in Header");
            }
            return responseCode;
        }

        @Override
        public boolean isOk() throws IOException {
            return getResponseCode() == 200;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getResponseMessage() throws IOException {
            if (errorInHeader) {
                throw new IOException("Connection Timeout in Header");
            }
            return message;
        }

        @Override
        public void close() {
        }
    }
}
