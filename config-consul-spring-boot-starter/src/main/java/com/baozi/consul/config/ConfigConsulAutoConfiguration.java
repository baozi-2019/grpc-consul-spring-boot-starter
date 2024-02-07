package com.baozi.consul.config;

import com.baozi.consul.common.Constant;
import com.baozi.consul.config.properties.ConsulProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(Constant.CONFIG_PREFIX + ".config.enable")
@EnableConfigurationProperties(ConsulProperties.class)
public class ConfigConsulAutoConfiguration {
}
