package com.capable.physiciandss.services;

import com.capable.physiciandss.configuration.WebClientConfig;
import com.capable.physiciandss.model.gocom.PingResponse;
import com.capable.physiciandss.model.gocom.ReferenceHelper;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Klasa zapewniająca komunikację z komponentem GoCom
 */
@Service
public class GoComService extends RootService {

    WebClient webClient;

    /**
     * Konfiguruje webClient oraz inicjalizuje loggera
     */
    public GoComService() {
        log = LoggerFactory.getLogger(this.getClass().getName());
        ApplicationContext context = new AnnotationConfigApplicationContext(WebClientConfig.class);
        webClient = context.getBean("webClientGoCom", WebClient.class);
        log.info("GoComService has been created");
    }

    /**
     * @param medicationRequestReference Referencja na receptę dla której chcemy sprawdzić możliwe konflikty
     * @return Obiekt zawierający informację czy GoCom rozwiązał jakiekolwiek problemy
     */
    public Mono<PingResponse> askGoComToCheckForConflicts(ReferenceHelper reference) {
        return webClient.post()
                .uri("/Ping")
                .body(Mono.just(reference), ReferenceHelper.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "PingGoCom"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("PingGoCom"))
                .bodyToMono(PingResponse.class);

    }
}
