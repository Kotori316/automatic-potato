package com.kotori316.ap;

import com.kotori316.ap.api.HttpReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

final class FakeHttpReader implements HttpReader {
    private final BiFunction<URI, String, HttpResponse> responseGenerator;

    FakeHttpReader(BiFunction<URI, String, HttpResponse> responseGenerator) {
        this.responseGenerator = responseGenerator;
    }

    FakeHttpReader(Function<URI, HttpResponse> responseGenerator) {
        this.responseGenerator = (uri, _) -> responseGenerator.apply(uri);
    }

    @Override
    public HttpResponse read(URI uri, String method, Map<String, String> headers) throws IOException {
        try {
            return responseGenerator.apply(uri, method);
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
        private final boolean errorInHeader;
        private final boolean errorInBody;

        FakeHttpResponse(int responseCode, String contentType, String body) {
            this(responseCode, contentType, body, false, false);
        }

        FakeHttpResponse(int responseCode, String contentType, String body, boolean errorInHeader, boolean errorInBody) {
            this.responseCode = responseCode;
            this.contentType = contentType;
            this.body = body.getBytes();
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
        public void close() {
        }
    }
}
