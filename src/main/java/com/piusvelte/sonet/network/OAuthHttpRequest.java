package com.piusvelte.sonet.network;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import oauth.signpost.http.HttpRequest;

/**
 * Created by bemmanuel on 5/15/15.
 */
public class OAuthHttpRequest implements HttpRequest {
    private static final String DEFAULT_CONTENT_TYPE = "application/json";

    private Request request;
    private String contentType;

    public OAuthHttpRequest(Request request) {
        this(request, DEFAULT_CONTENT_TYPE);
    }

    public OAuthHttpRequest(Request request, String contentType) {
        this.request = request;
        this.contentType = contentType;
    }

    @Override
    public Map<String, String> getAllHeaders() {
        HashMap<String, String> headers = new HashMap<>();

        Headers requestHeaders = request.headers();
        if (requestHeaders != null) {
            int size = requestHeaders.size();

            for (int headerIndex = 0; headerIndex < size; headerIndex++) {
                headers.put(requestHeaders.name(headerIndex), requestHeaders.value(headerIndex));
            }
        }

        return headers;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getHeader(String key) {
        Headers requestHeaders = request.headers();
        if (requestHeaders != null) {
            int size = requestHeaders.size();

            for (int headerIndex = 0; headerIndex < size; headerIndex++) {
                if (key.equals(requestHeaders.name(headerIndex))) {
                    return requestHeaders.value(headerIndex);
                }
            }
        }

        return null;
    }

    @Override
    public InputStream getMessagePayload() throws IOException {
        throw new RuntimeException(new UnsupportedOperationException());
    }

    @Override
    public String getMethod() {
        return request.method();
    }

    @Override
    public String getRequestUrl() {
        return request.urlString();
    }

    @Override
    public void setHeader(String key, String value) {
        request = new Request.Builder()
                .url(request.urlString())
                .method(request.method(), request.body())
                .headers(request.headers())
                .header(key, value)
                .tag(request.tag())
                .build();
    }

    @Override
    public void setRequestUrl(String url) {
        throw new RuntimeException(new UnsupportedOperationException());
    }

    @Override
    public Request unwrap() {
        return request;
    }
}
