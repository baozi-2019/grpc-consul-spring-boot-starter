package com.baozi.consul.discovery;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.common.Constant;
import com.baozi.consul.discovery.annotations.GrpcReference;
import com.baozi.consul.discovery.processor.GrpcClientBeanPostProcessor;
import com.baozi.consul.discovery.properties.ConsulProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn({"consulClient"})
@ConditionalOnClass(GrpcReference.class)
@EnableConfigurationProperties(ConsulProperties.class)
@ConditionalOnProperty(Constant.CONFIG_PREFIX + ".discovery.client")
public class GrpcConsulCustomerAutoConfiguration {

    @Bean
    public GrpcClientBeanPostProcessor grpcClientBeanFactory(
            ConsulProperties consulProperties,
            ConsulClient consulClient
            ) {
        return new GrpcClientBeanPostProcessor(consulProperties, consulClient);
    }
}
