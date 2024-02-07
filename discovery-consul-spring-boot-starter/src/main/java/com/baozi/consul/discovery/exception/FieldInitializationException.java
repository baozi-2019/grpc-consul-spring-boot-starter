package com.baozi.consul.discovery.exception;

import org.springframework.beans.BeansException;

import java.io.Serial;

public class FieldInitializationException extends BeansException {
    @Serial
    private static final long serialVersionUID = -8263341449914628595L;

    public FieldInitializationException(String msg) {
        super(msg);
    }

    public FieldInitializationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
