package com.kotori316.ap.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public interface HttpReader {
    /**
     * @param uri     the URI to access
     * @param method  the HTTP method (e.g., GET)
     * @param headers the HTTP headers
     * @return the response
     * @throws IOException if an I/O error occurs
     */
    @NotNull
    HttpResponse read(@NotNull URI uri, @NotNull String method, @NotNull Map<String, String> headers) throws IOException;

    /**
     * @return the HttpReader instance from {@link java.util.ServiceLoader}
     * @throws NoSuchElementException if no implementation is found
     */
    @NotNull
    static HttpReader load() {
        return StreamSupport.stream(ServiceLoader.load(HttpReader.class).spliterator(), false)
            .max(Comparator.comparingInt(HttpReader::priority))
            .orElseThrow(() -> new NoSuchElementException("No HttpReader implementation found"));
    }

    /**
     * The priority of this reader. A higher value has higher priority and is preferred by {@link #load()}.
     * Used to determine the implementation with {@link java.util.ServiceLoader}
     */
    default int priority() {
        return 0;
    }

    interface HttpResponse extends AutoCloseable {
        @NotNull
        InputStream getInputStream() throws IOException;

        int getResponseCode() throws IOException;

        boolean isOk() throws IOException;

        @Nullable
        String getContentType();

        @Override
        void close() throws IOException;
    }
}
