package com.baozi.consul.config.properties;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class ConfigProperties {
    private String host = "127.0.0.1";
    private int port = 8500;
    private boolean enable = true;
    @NestedConfigurationProperty
    private HttpClientProperties httpClient = new HttpClientProperties();

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

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public HttpClientProperties getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClientProperties httpClient) {
        this.httpClient = httpClient;
    }
}
