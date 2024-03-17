package com.baozi.consul.discovery.springboot.annotations;

import com.baozi.consul.enums.GrpcTransferEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcReference {
    String serviceName();

    GrpcTransferEnum grpcType() default GrpcTransferEnum.BLOCKING_STUB;

//    Class<?> serviceBuildClass();
}
