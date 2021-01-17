package com.capable.physiciandss.services;

import com.capable.physiciandss.configuration.WebClientConfig;
import com.capable.physiciandss.model.gocom.Ping;
import com.capable.physiciandss.model.gocom.PingResponse;
import com.capable.physiciandss.utils.Constants;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GoComService extends RootService {

    WebClient webClient;

    public GoComService() {
        log = LoggerFactory.getLogger(this.getClass().getName());
        ApplicationContext context = new AnnotationConfigApplicationContext(WebClientConfig.class);
        webClient = context.getBean("webClient", WebClient.class);
        log.info("GoComService has been created");
    }

    public Mono<PingResponse> askGoComToCheckForConflicts(Reference medicationRequestReference) {
        return webClient.post()
                .uri(Constants.GOCOM_BASE_URL + "/Ping")
                .body(Mono.just(new Ping(medicationRequestReference)), Ping.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "PingGoCom"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("PingGoCom"))
                .bodyToMono(PingResponse.class);

    }
}
