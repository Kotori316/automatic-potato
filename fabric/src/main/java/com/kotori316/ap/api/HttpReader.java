package com.kotori316.ap.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public interface HttpReader {
    /**
     * @param uri     the URI to access
     * @param method  the HTTP method (e.g., GET)
     * @param headers the HTTP headers
     * @return the response
     * @throws IOException if an I/O error occurs
     */
    HttpResponse read(URI uri, String method, Map<String, String> headers) throws IOException;

    interface HttpResponse extends AutoCloseable {
        InputStream getInputStream() throws IOException;

        int getResponseCode() throws IOException;

        String getContentType();

        String getResponseMessage() throws IOException;

        @Override
        void close() throws IOException;
    }
}
