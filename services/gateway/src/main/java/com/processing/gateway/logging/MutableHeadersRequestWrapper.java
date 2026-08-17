package com.processing.gateway.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * Request wrapper that allows gateway filters to add or override headers.
 */
public class MutableHeadersRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> mutableHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Creates a wrapper around the original servlet request.
     *
     * @param request request to wrap
     */
    public MutableHeadersRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * Adds or overrides a header value visible to downstream filters.
     *
     * @param name header name
     * @param value header value
     */
    public void setHeader(String name, String value) {
        mutableHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String value = mutableHeaders.get(name);

        return value != null ? value : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> headerNames = new HashSet<>(Collections.list(super.getHeaderNames()));
        headerNames.addAll(mutableHeaders.keySet());

        return Collections.enumeration(headerNames);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (mutableHeaders.containsKey(name)) {
            return Collections.enumeration(List.of(mutableHeaders.get(name)));
        }

        return super.getHeaders(name);
    }
}
