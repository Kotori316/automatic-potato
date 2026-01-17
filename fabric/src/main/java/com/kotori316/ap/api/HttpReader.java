package com.kotori316.ap.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

public interface HttpReader {
    /**
     * @param uri     the URI to access
     * @param method  the HTTP method (e.g., GET)
     * @param headers the HTTP headers
     * @return the response
     * @throws IOException if an I/O error occurs
     */
    HttpResponse read(URI uri, String method, Map<String, String> headers) throws IOException;

    /**
     * @return the HttpReader instance from {@link java.util.ServiceLoader}
     * @throws NoSuchElementException if no implementation is found
     */
    static HttpReader load() {
        ServiceLoader<HttpReader> loader = ServiceLoader.load(HttpReader.class);
        Iterator<HttpReader> iterator = loader.iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException("No HttpReader implementation found");
        }
        HttpReader best = iterator.next();
        while (iterator.hasNext()) {
            HttpReader current = iterator.next();
            if (current.priority() > best.priority()) {
                best = current;
            }
        }
        return best;
    }

    /**
     * The priority of this reader. Used to determine the implementation with {@link java.util.ServiceLoader}
     */
    default int priority() {
        return 0;
    }

    interface HttpResponse extends AutoCloseable {
        InputStream getInputStream() throws IOException;

        int getResponseCode() throws IOException;

        String getContentType();

        String getResponseMessage() throws IOException;

        @Override
        void close() throws IOException;
    }
}
