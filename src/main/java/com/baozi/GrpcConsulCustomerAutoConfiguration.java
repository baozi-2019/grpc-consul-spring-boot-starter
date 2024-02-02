package com.baozi;

import com.baozi.annotations.EnableDiscovery;
import com.baozi.annotations.GrpcReference;
import com.baozi.config.GrpcClientBeanPostProcessor;
import com.baozi.properties.GrpcConsulProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@ConditionalOnClass(GrpcReference.class)
@ConditionalOnBean(annotation = EnableDiscovery.class)
@DependsOn({"consulHttpClient"})
@EnableConfigurationProperties(GrpcConsulProperties.class)
@ConditionalOnProperty("grpc-consul.discovery.client")
public class GrpcConsulCustomerAutoConfiguration {

    @Bean
    public GrpcClientBeanPostProcessor grpcClientBeanFactory(
            GrpcConsulProperties grpcConsulProperties,
            @Qualifier("consulHttpClient") CloseableHttpClient consulHttpClient
    ) {
        return new GrpcClientBeanPostProcessor(grpcConsulProperties, consulHttpClient);
    }
}
