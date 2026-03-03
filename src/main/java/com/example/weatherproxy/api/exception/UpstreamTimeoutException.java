package com.example.weatherproxy.api.exception;

public class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException(String message) {
        super(message);
    }

    public UpstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
