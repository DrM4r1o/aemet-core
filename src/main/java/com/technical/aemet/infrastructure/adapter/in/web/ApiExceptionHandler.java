package com.technical.aemet.infrastructure.adapter.in.web;

import com.technical.aemet.application.exception.InvalidProviderResponseException;
import com.technical.aemet.application.exception.ProviderUnavailableException;
import com.technical.aemet.application.exception.RateLimitException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidProviderResponseException.class)
    ResponseEntity<Map<String, String>> invalidProviderResponse(InvalidProviderResponseException error) {
        log.warn("Provider returned an invalid response with status {}: {}", error.status(), error.description());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("code", "PROVIDER_INVALID_RESPONSE", "message", "Provider returned an invalid response"));
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    ResponseEntity<Map<String, String>> providerUnavailable(ProviderUnavailableException error) {
        var status = error instanceof RateLimitException ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.SERVICE_UNAVAILABLE;
        if (error instanceof RateLimitException) {
            log.warn("AEMET rate limit reached; data could not be loaded temporarily");
        } else {
            log.warn("Provider unavailable: {}", error.code(), error);
        }
        var response = ResponseEntity.status(status);
        if (error instanceof RateLimitException) response.header("Retry-After", "60");
        return response
                .body(Map.of("code", error.code(), "message", "Data is temporarily unavailable"));
    }

}
