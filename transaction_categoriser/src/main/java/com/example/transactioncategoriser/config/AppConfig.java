package com.example.transactioncategoriser.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class AppConfig {

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
