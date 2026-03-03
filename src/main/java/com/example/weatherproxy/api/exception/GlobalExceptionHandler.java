package com.example.weatherproxy.api.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROBLEM_BASE = "https://api.weather-proxy.example.com/problems/";

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(PROBLEM_BASE + "invalid-coordinates"));
        problem.setTitle("Invalid Coordinates");
        problem.setDetail(ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage()));
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return problem;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleBindException(WebExchangeBindException ex, ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(PROBLEM_BASE + "invalid-request"));
        problem.setTitle("Invalid Request");
        problem.setDetail(ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage()));
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return problem;
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ProblemDetail handleUpstreamTimeout(UpstreamTimeoutException ex, ServerWebExchange exchange) {
        log.warn("Upstream timeout: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.GATEWAY_TIMEOUT);
        problem.setType(URI.create(PROBLEM_BASE + "upstream-timeout"));
        problem.setTitle("Upstream Timeout");
        problem.setDetail("Weather data provider did not respond in time. Please retry.");
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return problem;
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ProblemDetail handleUpstreamUnavailable(UpstreamUnavailableException ex, ServerWebExchange exchange) {
        log.warn("Upstream unavailable: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setType(URI.create(PROBLEM_BASE + "upstream-unavailable"));
        problem.setTitle("Upstream Unavailable");
        problem.setDetail("Weather data provider is currently unavailable. Please retry later.");
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatusCode());
        problem.setType(URI.create(PROBLEM_BASE + "request-error"));
        problem.setTitle(ex.getReason() != null ? ex.getReason() : "Request Error");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception at {}", exchange.getRequest().getPath().value(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create(PROBLEM_BASE + "internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred.");
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return problem;
    }
}
