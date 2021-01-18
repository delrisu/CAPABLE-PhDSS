package com.capable.physiciandss.configuration;

import com.capable.physiciandss.hapi.Connection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.capable.physiciandss.utils.Constants.HAPI_BASE_URL;

/**
 * Konfiguracja połączenia z serwerem HAPI FHIR, pozwala na dependency injection
 */
@Configuration
public class HapiConnectionConfig {
    @Bean
    public Connection connection() {
        return new Connection(HAPI_BASE_URL);
    }
}
