package com.example.weatherproxy.api.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.ObjectError;
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
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "invalid-coordinates", "Invalid Coordinates", detail, exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleBindException(WebExchangeBindException ex, ServerWebExchange exchange) {
        String detail = ex.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid Request", detail, exchange);
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ProblemDetail handleUpstreamTimeout(UpstreamTimeoutException ex, ServerWebExchange exchange) {
        log.warn("Upstream timeout: {}", ex.getMessage());
        return buildProblem(HttpStatus.GATEWAY_TIMEOUT, "upstream-timeout", "Upstream Timeout",
                "Weather data provider did not respond in time. Please retry.", exchange);
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ProblemDetail handleUpstreamUnavailable(UpstreamUnavailableException ex, ServerWebExchange exchange) {
        log.warn("Upstream unavailable: {}", ex.getMessage());
        return buildProblem(HttpStatus.SERVICE_UNAVAILABLE, "upstream-unavailable", "Upstream Unavailable",
                "Weather data provider is currently unavailable. Please retry later.", exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        String title = ex.getReason() != null ? ex.getReason() : "Request Error";
        return buildProblem(ex.getStatusCode(), "request-error", title, ex.getMessage(), exchange);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception at {}", exchange.getRequest().getPath().value(), ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal Server Error",
                "An unexpected error occurred.", exchange);
    }

    private ProblemDetail buildProblem(HttpStatusCode status, String typeSlug, String title,
                                       String detail, ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(PROBLEM_BASE + typeSlug));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return problem;
    }
}
