package com.baozi.consul.discovery.exception;

import java.io.Serial;

public class DiscoveryException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6830276940618120075L;


    public DiscoveryException(String message) {
        super(message);
    }

    public DiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
