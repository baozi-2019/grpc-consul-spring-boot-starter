package com.baozi.consul.discovery.config;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.discovery.properties.DiscoveryProperties;
import com.baozi.consul.properties.ConsulProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
@EnableConfigurationProperties(com.baozi.consul.discovery.properties.ConsulProperties.class)
public class ConsulClientConfig {
    @Bean
    @ConditionalOnMissingBean
    public ConsulClient consulClient(
            com.baozi.consul.discovery.properties.ConsulProperties consulProperties
    ) throws URISyntaxException {
        ConsulProperties consulProp = new ConsulProperties();
        DiscoveryProperties discoveryProperties = consulProperties.getDiscovery();
        consulProp.setHost(discoveryProperties.getHost());
        consulProp.setPort(discoveryProperties.getPort());
        return new ConsulClient(consulProperties.getDiscovery().getHttpClient(), consulProp);
    }
}
