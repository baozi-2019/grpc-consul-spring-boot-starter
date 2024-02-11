package com.baozi.consul.discovery.processor;

import com.baozi.consul.discovery.annotations.GrpcAdvice;
import com.baozi.consul.discovery.annotations.GrpcExceptionHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GrpcExceptionProcessor implements InitializingBean, ApplicationContextAware {
    private ApplicationContext applicationContext;
    private Map<? extends Class<?>, Method> methodMap;

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> map = this.applicationContext.getBeansWithAnnotation(GrpcAdvice.class);
        this.methodMap = map.values().stream().map(e -> findAnnotatedMethods(e.getClass()))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v2
                ));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private Set<Map.Entry<Class<?>, Method>> findAnnotatedMethods(final Class<?> clazz) {
//        return MethodIntrospector.selectMethods(clazz, (ReflectionUtils.MethodFilter) method -> {
//            GrpcExceptionHandler annotation = method.getDeclaredAnnotation(GrpcExceptionHandler.class);
//            Class<?> parameterType = method.getParameterTypes()[0];
//            if (annotation == null || parameterType == null) return false;
//            Class<? extends Throwable> exceptionClass = annotation.value();
//            return exceptionClass.equals(parameterType);
//        });
        return Arrays.stream(clazz.getMethods()).filter(e -> e.isAnnotationPresent(GrpcExceptionHandler.class))
                .collect(Collectors.toUnmodifiableMap(e -> {
                    GrpcExceptionHandler grpcExceptionHandler = e.getDeclaredAnnotation(GrpcExceptionHandler.class);
                    Class<?>[] parameterTypes = e.getParameterTypes();
                    if (parameterTypes.length > 0 && Throwable.class.isAssignableFrom(parameterTypes[0]))
                        return parameterTypes[0];
                    else if (parameterTypes.length == 0 && grpcExceptionHandler.value() != null)
                        return grpcExceptionHandler.value();
                    else throw new IllegalArgumentException("GrpcExceptionHandler注解方法参数非法");
                }, e -> e)).entrySet();
    }

    public Map<? extends Class<?>, Method> getMethodMap() {
        return methodMap;
    }

    public void setMethodMap(Map<? extends Class<?>, Method> methodMap) {
        this.methodMap = methodMap;
    }
}
