package com.example.weatherproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import com.example.weatherproxy.config.WeatherProperties;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(WeatherProperties.class)
public class WeatherProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherProxyApplication.class, args);
    }
}
