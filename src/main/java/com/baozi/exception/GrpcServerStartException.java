package com.baozi.exception;

import java.io.Serial;

public class GrpcServerStartException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6830276940618120075L;

    public GrpcServerStartException(String message, Throwable cause) {
        super(message, cause);
    }
}
