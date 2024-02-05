package com.baozi.config;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.properties.ConsulProperties;
import com.baozi.properties.DiscoveryProperties;
import com.baozi.properties.GrpcConsulProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
@EnableConfigurationProperties(GrpcConsulProperties.class)
public class ConsulClientConfig {
    @Bean
    @ConditionalOnMissingBean
    public ConsulClient consulClient(
            GrpcConsulProperties grpcConsulProperties
    ) throws URISyntaxException {
        DiscoveryProperties discovery = grpcConsulProperties.getDiscovery();
        ConsulProperties consulProperties = new ConsulProperties();
        consulProperties.setHost(discovery.getHost());
        consulProperties.setPort(discovery.getPort());
        return new ConsulClient(grpcConsulProperties.getHttpClient(), consulProperties);
    }
}
