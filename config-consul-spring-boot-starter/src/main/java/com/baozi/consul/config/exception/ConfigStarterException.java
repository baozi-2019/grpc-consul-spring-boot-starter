package com.baozi.consul.config.exception;

import java.io.Serial;

public class ConfigStarterException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6830276940618120075L;

    public ConfigStarterException(String message) {
        super(message);
    }

    public ConfigStarterException(String message, Throwable cause) {
        super(message, cause);
    }
}
