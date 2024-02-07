package com.baozi.consul.discovery.exception;

import java.io.Serial;

public class IllegalGrpcSchemaException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -6445151566135435627L;

    public IllegalGrpcSchemaException(String message) {
        super(message);
    }
}
