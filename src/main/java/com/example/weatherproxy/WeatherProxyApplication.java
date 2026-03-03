package com.example.weatherproxy;

import com.example.weatherproxy.config.WeatherProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(WeatherProperties.class)
@OpenAPIDefinition(info = @Info(
        title = "Real Temperature Proxy API",
        version = "0.1.0",
        description = "Proxies Open-Meteo to return current temperature and wind speed " +
                      "for any coordinate. Responses are cached for 60 seconds."
))
public class WeatherProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherProxyApplication.class, args);
    }
}
