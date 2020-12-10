package com.capable.physiciandss.services;

import com.capable.physiciandss.utils.Constants;
import org.slf4j.Logger;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

public class RootService {

    protected Logger log;

    protected Mono<? extends Throwable> onError(ClientResponse response, String methodName) {
        IllegalStateException ex = new IllegalStateException(methodName + Constants.REQUEST_FAILED_MESSAGE
                + response.statusCode());
        log.debug(ex.getMessage());
        return Mono.error(ex);
    }

    protected Mono<? extends Throwable> onSuccess(String postEnact) {
        log.info(postEnact + Constants.REQUEST_SUCCEDED_MESSAGE);
        return Mono.empty();
    }
}
