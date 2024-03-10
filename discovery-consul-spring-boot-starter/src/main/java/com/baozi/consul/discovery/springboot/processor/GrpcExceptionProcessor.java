package com.baozi.consul.discovery.springboot.processor;

import com.baozi.consul.discovery.grpc.record.ClassInstanceWithMethodRecord;
import com.baozi.consul.discovery.springboot.annotations.GrpcAdvice;
import com.baozi.consul.discovery.springboot.annotations.GrpcExceptionHandler;
import com.baozi.consul.discovery.springboot.exception.DiscoveryStarterException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GrpcExceptionProcessor implements InitializingBean, ApplicationContextAware {
    private ApplicationContext applicationContext;
    private Map<Class<?>, ClassInstanceWithMethodRecord> methodMap;

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> map = this.applicationContext.getBeansWithAnnotation(GrpcAdvice.class);
        this.methodMap = map.values().stream().map(e -> this.findAnnotatedMethods(e.getClass()))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v2
                ));
    }

    private Set<Map.Entry<Class<?>, ClassInstanceWithMethodRecord>> findAnnotatedMethods(final Class<?> clazz) {
        try {
            final Object instance = clazz.getConstructor().newInstance();
            return Arrays.stream(clazz.getMethods()).filter(e -> e.isAnnotationPresent(GrpcExceptionHandler.class))
                    .collect(Collectors.toUnmodifiableMap(e -> {
                        GrpcExceptionHandler grpcExceptionHandler = e.getDeclaredAnnotation(GrpcExceptionHandler.class);
                        Class<?>[] parameterTypes = e.getParameterTypes();
                        if (parameterTypes.length > 0 && Throwable.class.isAssignableFrom(parameterTypes[0]))
                            return parameterTypes[0];
                        else if (parameterTypes.length == 0 && grpcExceptionHandler.value() != null)
                            return grpcExceptionHandler.value();
                        else throw new IllegalArgumentException("GrpcExceptionHandler注解方法参数非法");
                    }, e -> new ClassInstanceWithMethodRecord(instance, e))).entrySet();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new DiscoveryStarterException(clazz.getCanonicalName() + "实例化失败", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Map<Class<?>, ClassInstanceWithMethodRecord> getMethodMap() {
        return methodMap;
    }
}
