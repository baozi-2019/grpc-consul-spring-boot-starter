package com.baozi.consul.discovery.processor;

import com.baozi.consul.ConsulClient;
import com.baozi.consul.discovery.annotations.GrpcReference;
import com.baozi.consul.discovery.exception.FieldInitializationException;
import com.baozi.consul.discovery.grpc.resover.provider.ConsulNameResolverProvider;
import com.baozi.consul.discovery.properties.ConsulProperties;
import com.baozi.consul.discovery.properties.DiscoveryProperties;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverRegistry;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GrpcClientBeanPostProcessor implements BeanPostProcessor {
    private final ConcurrentHashMap<String, Channel> grpcChannelMap;
    private final ConcurrentHashMap<String, Object> grpcClientMap;

    public GrpcClientBeanPostProcessor(
            ConsulProperties consulProperties,
            ConsulClient consulClient
    ) {
        DiscoveryProperties discoveryProperties = consulProperties.getDiscovery();

        // grpc服务注册器注入
        NameResolverRegistry.getDefaultRegistry().register(ConsulNameResolverProvider.newBuilder(consulClient)
                .withResolveInterval(Duration.ofSeconds(5)).build());

        String[] registerServiceNames = discoveryProperties.getRegisterServiceNames();
        // grpc channel map初始化
        this.grpcChannelMap = new ConcurrentHashMap<>();
        for (String serviceName : registerServiceNames) {
            // 注入对应服务提供器
            ManagedChannel channel = ManagedChannelBuilder.forTarget("consul://" + discoveryProperties.getHost()
                            + ":" + discoveryProperties.getPort() + "/" + serviceName)
                    .overrideAuthority(serviceName)
                    .defaultLoadBalancingPolicy("round_robin")
                    .disableRetry()
                    .keepAliveTime(20, TimeUnit.SECONDS)
                    .usePlaintext().build();
            this.grpcChannelMap.put(serviceName, channel);
        }
        // grpc客户端map初始化
        this.grpcClientMap = new ConcurrentHashMap<>();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            GrpcReference grpcReference = field.getAnnotation(GrpcReference.class);
            if (grpcReference == null) continue;
            field.setAccessible(true);
            // 生成grpc client key
            String grpcClientKey = bean.getClass().getTypeName() + "-" + field.getName();
            Object blockingStub;
            if ((blockingStub = this.grpcClientMap.get(grpcClientKey)) == null) {
                // 缓存中不存在client 生成一个
                blockingStub = this.generateGrpcClient(grpcReference);
                this.grpcClientMap.put(grpcClientKey, blockingStub);
            }
            try {
                field.set(bean, blockingStub);
            } catch (IllegalAccessException e) {
                throw new FieldInitializationException("属性设置失败", e);
            }
        }
        return bean;
    }

    private Object generateGrpcClient(GrpcReference grpcReference) {
        try {
            // 实例化GRPC对象
            String localServiceName = grpcReference.serviceName();
            Class<?> serviceBuildClass = grpcReference.serviceBuildClass();
            Channel grpcChannel = this.grpcChannelMap.get(localServiceName);
            Method serviceBuildClassMethod = serviceBuildClass.getMethod("newBlockingStub", Channel.class);
            return serviceBuildClassMethod.invoke(null, grpcChannel);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BeanCreationException("grpc创建失败", e);
        }
    }
}
