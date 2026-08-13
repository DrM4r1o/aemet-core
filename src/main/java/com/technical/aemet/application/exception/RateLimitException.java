package com.technical.aemet.application.exception;

public class RateLimitException extends ProviderUnavailableException {
    public RateLimitException(Throwable cause) {
        super("AEMET_RATE_LIMITED", cause);
    }
}
