package com.capable.physiciandss.configuration;

import com.capable.physiciandss.utils.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Konfiguracja zapewniająca działanie WebClient, pozwala na dependency injection
 */
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClientDeontics() {
        return WebClient.builder()
                .baseUrl(Constants.DEONTICS_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-apikey", Constants.X_APIKEY)
                .build();
    }
    @Bean
    public WebClient webClientGoCom() {
        return WebClient.builder()
                .baseUrl(Constants.GOCOM_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
