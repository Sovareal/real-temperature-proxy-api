package com.example.weatherproxy.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final int PENDING_ACQUIRE_TIMEOUT_SECONDS = 5;

    @Bean
    public WebClient openMeteoWebClient(WebClient.Builder builder, WeatherProperties props) {
        WeatherProperties.UpstreamProperties upstream = props.upstream();

        ConnectionProvider connectionProvider = ConnectionProvider.builder("open-meteo")
                .maxConnections(upstream.maxConnections())
                .pendingAcquireTimeout(Duration.ofSeconds(PENDING_ACQUIRE_TIMEOUT_SECONDS))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) upstream.connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(upstream.timeoutMs()));

        return builder
                .baseUrl(upstream.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
