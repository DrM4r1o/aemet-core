package com.technical.aemet.application.exception;

public class InvalidProviderResponseException extends RuntimeException {
    private final int status;
    private final String description;

    public InvalidProviderResponseException(Integer status, String description) {
        super(description);
        this.status = status == null ? 502 : status;
        this.description = description == null ? "Provider returned an invalid response" : description;
    }

    public int status() {
        return status;
    }

    public String description() {
        return description;
    }
}
