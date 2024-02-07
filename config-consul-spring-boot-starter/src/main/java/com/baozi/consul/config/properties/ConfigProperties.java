package com.baozi.consul.config.properties;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class ConfigProperties {
    private String host = "127.0.0.1";
    private int port = 8500;
    @NestedConfigurationProperty
    private HttpClientProperties httpClient;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public HttpClientProperties getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClientProperties httpClient) {
        this.httpClient = httpClient;
    }
}
