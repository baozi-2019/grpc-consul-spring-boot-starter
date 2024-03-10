package com.baozi.consul.config;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.bean.kv.KVStore;
import com.baozi.consul.config.exception.ConfigException;
import com.baozi.consul.config.properties.ConfigProperties;
import com.baozi.consul.exception.ConsulClientException;
import com.baozi.consul.properties.ConsulProperties;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public class ConfigConsul {
    private ConfigProperties configProperties;
    private ConsulClient consulClient;

    private ConfigConsul(ConfigProperties configProperties, ConsulClient consulClient) {
        this.configProperties = configProperties;
        this.consulClient = Optional.ofNullable(consulClient).orElseGet(() -> {
            ConsulProperties consulProperties = new ConsulProperties();
            consulProperties.setPort(this.configProperties.getPort());
            consulProperties.setHost(this.configProperties.getHost());
            try {
                return new ConsulClient(this.configProperties.getHttpClient(), consulProperties);
            } catch (URISyntaxException e) {
                throw new ConfigException("consul客户端初始化失败", e);
            }
        });
    }

    public String[] getRemoteConfig(String prefix, String[] configFileNames) throws ConsulClientException {
        int length = configFileNames.length;
        String[] configs = new String[length];
        for (int i = 0; i < length; i++) {
            List<KVStore> kvStores = this.consulClient.readKey(prefix + configFileNames[i]);
            if (!kvStores.isEmpty()) {
                KVStore kvStore = kvStores.get(0);
                configs[i] = kvStore.decodeValue();
            }
        }
        return configs;
    }

    public static class Build {
        private ConfigProperties configProperties;
        private ConsulClient consulClient;

        public Build configProperties(ConfigProperties configProperties) {
            this.configProperties = configProperties;
            return this;
        }

        public Build consulClient(ConsulClient consulClient) {
            this.consulClient = consulClient;
            return this;
        }

        public ConfigConsul build() {
            this.configProperties = Optional.ofNullable(this.configProperties).orElse(new ConfigProperties());
            return new ConfigConsul(this.configProperties, this.consulClient);
        }

        public ConfigProperties getConfigProperties() {
            return configProperties;
        }

        public ConsulClient getConsulClient() {
            return consulClient;
        }
    }
}
