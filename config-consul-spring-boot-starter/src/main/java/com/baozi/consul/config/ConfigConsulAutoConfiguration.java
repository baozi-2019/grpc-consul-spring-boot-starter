package com.baozi.consul.config;

import com.baozi.consul.config.properties.ConsulProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConsulProperties.class)
public class ConfigConsulAutoConfiguration {
}
