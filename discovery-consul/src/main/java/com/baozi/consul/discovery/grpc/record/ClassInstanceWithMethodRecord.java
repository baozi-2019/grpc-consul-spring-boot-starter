package com.baozi.consul.discovery.grpc.record;

import java.lang.reflect.Method;

public record ClassInstanceWithMethodRecord(
        Object instance,
        Method method
) {}
