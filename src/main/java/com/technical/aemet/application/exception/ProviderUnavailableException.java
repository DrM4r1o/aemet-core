package com.technical.aemet.application.exception;

public class ProviderUnavailableException extends RuntimeException {
    private final String code;

    public ProviderUnavailableException(String code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
