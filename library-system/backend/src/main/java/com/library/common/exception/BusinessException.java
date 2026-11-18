package com.library.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final transient Object errors;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public BusinessException(String code, String message, HttpStatus status, Object errors) {
        super(message);
        this.code = code;
        this.status = status;
        this.errors = errors;
    }
}
