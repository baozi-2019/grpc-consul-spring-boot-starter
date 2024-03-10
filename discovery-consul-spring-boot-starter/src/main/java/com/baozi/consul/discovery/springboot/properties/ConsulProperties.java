package com.baozi.consul.discovery.springboot.properties;

import com.baozi.consul.discovery.properties.DiscoveryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.baozi.consul.common.Constant.CONFIG_PREFIX;

@ConfigurationProperties(prefix = CONFIG_PREFIX)
public class ConsulProperties {
    @NestedConfigurationProperty
    private DiscoveryProperties discovery = new DiscoveryProperties();

    public DiscoveryProperties getDiscovery() {
        return discovery;
    }

    public void setDiscovery(DiscoveryProperties discovery) {
        this.discovery = discovery;
    }
}
