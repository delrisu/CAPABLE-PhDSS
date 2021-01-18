package com.capable.physiciandss.services;

import com.capable.physiciandss.utils.Constants;
import org.slf4j.Logger;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * Klasa definiuje zachowanie serwisów opartych o Spring WebClient w przypadku,
 * gdy zapytanie HTTP zakończy się sukcesem lub błędem.
 */
public class RootService {

    protected Logger log;

    /**
     * @param response Obiekt zwracany w wyniku zapytań HTTP
     * @param methodName Nazwa metody, w której zapytanie HTTP się nie powiodło i która zostanie wpisana w logach
     * @return
     */
    protected Mono<? extends Throwable> onError(ClientResponse response, String methodName) {
        IllegalStateException ex = new IllegalStateException(methodName + Constants.REQUEST_FAILED_MESSAGE
                + response.statusCode());
        log.debug(ex.getMessage());
        return Mono.error(ex);
    }

    /**
     * @param methodName Nazwa metody, w której zapytanie HTTP się powiodło i która zostanie wpisana w logach
     * @return
     */
    protected Mono<? extends Throwable> onSuccess(String methodName) {
        log.info(methodName + Constants.REQUEST_SUCCEDED_MESSAGE);
        return Mono.empty();
    }
}
