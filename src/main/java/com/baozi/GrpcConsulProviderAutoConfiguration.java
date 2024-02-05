package com.baozi;

import com.baozi.annotations.EnableRegister;
import com.baozi.annotations.GrpcService;
import com.baozi.consul.ConsulClient;
import com.baozi.processor.GrpcConsulProviderRoundProcessor;
import com.baozi.properties.GrpcConsulProperties;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn({"consulClient"})
@ConditionalOnClass(GrpcService.class)
@ConditionalOnBean(annotation = EnableRegister.class)
@AutoConfigureOrder(value = Integer.MAX_VALUE)
@EnableConfigurationProperties(GrpcConsulProperties.class)
@ConditionalOnProperty("grpc-consul.discovery.register")
public class GrpcConsulProviderAutoConfiguration {

    @Bean
    public GrpcConsulProviderRoundProcessor grpcConsulProviderRoundProcessor(
            GrpcConsulProperties grpcConsulProperties,
            ConsulClient consulClient,
            ApplicationContext applicationContext) {
        return new GrpcConsulProviderRoundProcessor(grpcConsulProperties, consulClient, applicationContext);
    }
}
