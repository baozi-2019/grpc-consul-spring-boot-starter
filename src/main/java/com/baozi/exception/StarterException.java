package com.baozi.exception;

import java.io.Serial;

public class StarterException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6830276940618120075L;

    public StarterException(String message, Throwable cause) {
        super(message, cause);
    }
}
