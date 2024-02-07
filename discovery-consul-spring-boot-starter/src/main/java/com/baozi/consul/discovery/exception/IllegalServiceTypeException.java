package com.baozi.consul.discovery.exception;

import java.io.Serial;

public class IllegalServiceTypeException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = -889549174467516525L;

    public IllegalServiceTypeException(String message) {
        super(message);
    }
}
