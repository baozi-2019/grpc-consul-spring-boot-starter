package com.baozi.exception;

import java.io.Serial;

public class IllegalSyntaxException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 4647139668286837753L;

    public IllegalSyntaxException(Throwable cause) {
        super(cause);
    }
}
