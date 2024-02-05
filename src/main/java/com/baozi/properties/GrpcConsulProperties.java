package com.baozi.properties;

import com.baozi.consul.properties.HttpClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "grpc-consul")
public class GrpcConsulProperties {
    @NestedConfigurationProperty
    private HttpClientProperties httpClient;
    @NestedConfigurationProperty
    private DiscoveryProperties discovery;

    public HttpClientProperties getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClientProperties httpClient) {
        this.httpClient = httpClient;
    }

    public DiscoveryProperties getDiscovery() {
        return discovery;
    }

    public void setDiscovery(DiscoveryProperties discovery) {
        this.discovery = discovery;
    }
}
