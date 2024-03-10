package com.baozi.consul.discovery.springboot.exception;

import java.io.Serial;

public class DiscoveryStarterException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6830276940618120075L;

    public DiscoveryStarterException(String message, Throwable cause) {
        super(message, cause);
    }
}
