package com.baozi.annotations;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcReference {
    String serviceName();

    Class<?> serviceBuildClass();

    // 超时时间，单位（毫秒）
    long timeout() default 5000;
}