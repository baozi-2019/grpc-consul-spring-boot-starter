package com.baozi;

import com.baozi.annotations.EnableDiscovery;
import com.baozi.annotations.GrpcReference;
import com.baozi.consul.ConsulClient;
import com.baozi.processor.GrpcClientBeanPostProcessor;
import com.baozi.properties.GrpcConsulProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn({"consulClient"})
@ConditionalOnClass(GrpcReference.class)
@ConditionalOnBean(annotation = EnableDiscovery.class)
@EnableConfigurationProperties(GrpcConsulProperties.class)
@ConditionalOnProperty("grpc-consul.discovery.client")
public class GrpcConsulCustomerAutoConfiguration {

    @Bean
    public GrpcClientBeanPostProcessor grpcClientBeanFactory(
            GrpcConsulProperties grpcConsulProperties,
            ConsulClient consulClient
            ) {
        return new GrpcClientBeanPostProcessor(grpcConsulProperties, consulClient);
    }
}
