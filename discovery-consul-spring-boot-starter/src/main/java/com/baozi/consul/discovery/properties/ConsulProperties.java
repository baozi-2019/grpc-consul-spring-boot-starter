package com.baozi.consul.discovery.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.baozi.consul.common.Constant.CONFIG_PREFIX;

@ConfigurationProperties(prefix = CONFIG_PREFIX)
public class ConsulProperties {
    @NestedConfigurationProperty
    private DiscoveryProperties discovery;

    public DiscoveryProperties getDiscovery() {
        return discovery;
    }

    public void setDiscovery(DiscoveryProperties discovery) {
        this.discovery = discovery;
    }
}
