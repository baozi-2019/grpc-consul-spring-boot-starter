package com.baozi.consul.config.processor;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.bean.kv.KVStore;
import com.baozi.consul.common.Constant;
import com.baozi.consul.config.exception.ConfigStarterException;
import com.baozi.consul.config.properties.ConfigProperties;
import com.baozi.consul.config.properties.ConsulProperties;
import com.baozi.consul.exception.ConsulClientException;
import com.google.common.base.CaseFormat;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    private static final String INCLUDE_PROFILES_PROPERTY_NAME_KEY = "spring.profiles.include";
    private static final String SERVICE_NAME_KEY = "spring.application.name";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ClassPathResource path = new ClassPathResource("application.yaml");
        if (!path.exists()) path = new ClassPathResource("/config/application.yaml");
        if (!path.exists()) throw new ConfigStarterException("找不到配置中心相关配置");
        PropertySource<?> propertySource;
        try {
            propertySource = this.loader.load("remote-config-application", path).get(0);
        } catch (IOException e) {
            throw new ConfigStarterException("配置中心配置加载失败");
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        Optional<PropertySource<?>> propertySourceOptional = propertySources.stream().filter(e -> e.getName().contains("application")).findFirst();
        if (propertySourceOptional.isPresent()) {
            propertySources.addBefore(propertySourceOptional.get().getName(), propertySource);
        } else {
            propertySources.addLast(propertySource);
        }

        // 获取consul client
        com.baozi.consul.properties.ConsulProperties consulProperties = new com.baozi.consul.properties.ConsulProperties();
        ConsulProperties springConsulProperties = new ConsulProperties();
        try {
            convert(Constant.CONFIG_PREFIX, springConsulProperties.getClass().getDeclaredFields(), springConsulProperties, environment);
        } catch (Exception e) {
            throw new ConfigStarterException("配置中心配置读取失败", e);
        }
        String serviceName = environment.getProperty(SERVICE_NAME_KEY, String.class);
        if (serviceName == null) throw new ConfigStarterException("服务名未配置");
        try {
            ConfigProperties configProperties = springConsulProperties.getConfig();
            consulProperties.setHost(configProperties.getHost());
            consulProperties.setPort(configProperties.getPort());
            ConsulClient consulClient = new ConsulClient(configProperties.getHttpClient(), consulProperties);
            // 获取远程配置，添加进spring boot容器
            int i = 0;
            List<String> includeProfilesList = new LinkedList<>();
            while (true){
                Optional<String> includeProfileOptional = Optional.ofNullable(environment.getProperty(INCLUDE_PROFILES_PROPERTY_NAME_KEY + "[" + i++ + "]", String.class));
                if (includeProfileOptional.isEmpty()) break;
                includeProfilesList.add(includeProfileOptional.get());
            }
            for (String includeProfile : includeProfilesList) {
                String profileName = "application-" + includeProfile + ".yaml";
                List<KVStore> kvStoreList = consulClient.readKey(serviceName + "/" + profileName);
                if (kvStoreList == null) continue;
                // 添加远程配置到spring容器
                if (propertySourceOptional.isPresent())
                    propertySources.addBefore(propertySourceOptional.get().getName(), this.loader.load(profileName,
                            new ByteArrayResource(kvStoreList.get(0).decodeValue().getBytes(StandardCharsets.UTF_8))).get(0));
                else
                    propertySources.addLast(this.loader.load(profileName, new ByteArrayResource(kvStoreList.get(0).decodeValue().getBytes(StandardCharsets.UTF_8))).get(0));
            }
        } catch (URISyntaxException e) {
            throw new ConfigStarterException("consul client初始化失败", e);
        } catch (ConsulClientException | IOException e) {
            throw new ConfigStarterException("获取远程配置失败", e);
        }
    }

    private void convert(String prefix, Field[] fields, Object fieldValue, ConfigurableEnvironment environment) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        for (Field field : fields) {
            field.setAccessible(true);
            String snakeCaseName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.getName());
            String nowFieldPrefix = prefix + "." + snakeCaseName;
            if (field.getType().getClassLoader() == null) {
                // read to bean
                Object property = environment.getProperty(nowFieldPrefix, field.getType());
                if (property != null) field.set(fieldValue, property);
                continue;
            }
            Object nextField = field.get(fieldValue);
            if (nextField == null) {
                // 如果字段是空，创建一个实例
                nextField = ((Class) field.getGenericType()).getDeclaredConstructor().newInstance();
                field.set(fieldValue, nextField);
            }
            // 找父类
            Class<?> superclass = ((Class) field.getGenericType()).getSuperclass();
            if (superclass.getClassLoader() != null) {
                convert(nowFieldPrefix, superclass.getDeclaredFields(), nextField, environment);
            }
            // 找字段
            convert(nowFieldPrefix, ((Class) field.getGenericType()).getDeclaredFields(), nextField, environment);
        }
    }
}
