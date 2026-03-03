package com.example.weatherproxy.config;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestIdFilter implements WebFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders()
                .getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);

        final String finalRequestId = requestId;
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(MDC_KEY, finalRequestId))
                .doFirst(() -> MDC.put(MDC_KEY, finalRequestId))
                .doFinally(signal -> MDC.remove(MDC_KEY));
    }
}
