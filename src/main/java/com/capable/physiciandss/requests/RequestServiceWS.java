package com.capable.physiciandss.requests;


import com.capable.physiciandss.configuration.WebClientConfig;
import com.capable.physiciandss.model.Connect;
import com.capable.physiciandss.model.Enactment;
import com.capable.physiciandss.model.ItemData;
import com.capable.physiciandss.model.Pathway;
import com.capable.physiciandss.utils.Constants;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.logging.Logger;

@Service
public class RequestServiceWS {

    private final Logger log = Logger.getLogger(getClass().getName());
    WebClient webClient;


    public RequestServiceWS() {
        ApplicationContext context = new AnnotationConfigApplicationContext(WebClientConfig.class);
        webClient = context.getBean("webClient", WebClient.class);
    }

    public Mono<Enactment[]> getEnactments() {
        return webClient.get()
                .uri(Constants.PRS_API_URL + "/Enactments")
                .retrieve()
                .bodyToMono(Enactment[].class);
    }

    public Mono<Pathway[]> getPathway() {
        return webClient.get()
                .uri(Constants.PRS_API_URL + "/Pathways")
                .retrieve()
                .bodyToMono(Pathway[].class);
    }

    public Mono<ItemData[]> getData(String sessionId) {
        return webClient.get()
                .uri(Constants.DRE_API_URL + "/Data")
                .header("x-dresessionid", sessionId)
                .retrieve()
                .bodyToMono(ItemData[].class);
    }

    public Mono<Connect> getConnection(String enactmentId) {
        return webClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(Constants.DRE_API_URL + "/Connect")
                                .queryParam("enactmentid", enactmentId)
                                .build())
                .retrieve()
                .bodyToMono(Connect.class);
    }
}
