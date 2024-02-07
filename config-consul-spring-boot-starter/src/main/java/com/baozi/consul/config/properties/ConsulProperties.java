package com.baozi.consul.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.baozi.consul.common.Constant.CONFIG_PREFIX;

@ConfigurationProperties(prefix = CONFIG_PREFIX)
public class ConsulProperties {
    @NestedConfigurationProperty
    private ConfigProperties config;

    public ConfigProperties getConfig() {
        return config;
    }

    public void setConfig(ConfigProperties config) {
        this.config = config;
    }
}
