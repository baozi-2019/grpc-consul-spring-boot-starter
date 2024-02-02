package com.baozi.exception;

import java.io.Serial;

public class ConsulException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -1441584694978727617L;

    public ConsulException(String message) {
        super(message);
    }

    public ConsulException(String message, Throwable cause) {
        super(message, cause);
    }
}
