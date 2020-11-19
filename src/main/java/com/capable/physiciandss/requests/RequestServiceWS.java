package com.capable.physiciandss.requests;


import com.capable.physiciandss.configuration.WebClientConfig;
import com.capable.physiciandss.model.get.*;
import com.capable.physiciandss.model.post.EnactBody;
import com.capable.physiciandss.model.post.EnactOutput;
import com.capable.physiciandss.model.put.*;
import com.capable.physiciandss.utils.Constants;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
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
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("getEnactments failed with error code: " + response.statusCode()));
                })
                .bodyToMono(Enactment[].class);
    }

    public Mono<Pathway[]> getPathway(boolean temp) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.PRS_API_URL + "/Pathways")
                        .queryParam("temp", temp)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("getPathway failed with error code: " + response.statusCode()));
                })
                .bodyToMono(Pathway[].class);
    }

    public Mono<ItemData[]> getData(String enquiryName, String sessionId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.DRE_API_URL + "/Data")
                        .queryParam("enquiryname", enquiryName)
                        .queryParam("metaprops", "true")
                        .build())
                .header("x-dresessionid", sessionId)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("getData failed with error code: " + response.statusCode()));
                })
                .bodyToMono(ItemData[].class);
    }

    public Mono<Connect> getConnect(String enactmentId) {
        return webClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(Constants.DRE_API_URL + "/Connect")
                                .queryParam("enactmentid", enactmentId)
                                .build())
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("getConnect failed with error code: " + response.statusCode()));
                })
                .bodyToMono(Connect.class);
    }

    public Mono<PlanTask> getTask(String runtimeid, String sessionId) {
        return webClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(Constants.DRE_API_URL + "/Task")
                                .queryParam("runtimeid", runtimeid)
                                .build())
                .header("x-dresessionid", sessionId)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("getTask failed with error code: " + response.statusCode()));
                })
                .bodyToMono(PlanTask.class);
    }

    public Mono<PlanTask[]> getPlanTasks(String state, String sessionId) {
        return webClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(Constants.DRE_API_URL + "/PlanTasks")
                                .queryParam("state", state)
                                .queryParam("root", "true")
                                .queryParam("recursive", "true")
                                .queryParam("flat", "true")
                                .queryParam("metaprops", "true")
                                .build())
                .header("x-dresessionid", sessionId)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("getPlanTasks failed with error code: " + response.statusCode()));
                })
                .bodyToMono(PlanTask[].class);
    }

    public Mono<EnactOutput> postEnact(String pathwayid, String patientid) {
        return webClient.post()
                .uri(Constants.DRE_API_URL + "/Enact")
                .body(Mono.just(new EnactBody(pathwayid, patientid)), EnactBody.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("postEnact failed with error code: " + response.statusCode()));
                })
                .bodyToMono(EnactOutput.class);
    }

    public Mono<DataValueOutput> putDataValue(String name, String value, String sessionId) {
        return webClient.put()
                .uri(Constants.DRE_API_URL + "/DataValue")
                .header("x-dresessionid", sessionId)
                .body(Mono.just(new DataValueBody(name, value)), DataValueBody.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("putDataValue failed with error code: " + response.statusCode()));
                })
                .bodyToMono(DataValueOutput.class);
    }

    public Mono<ConfirmTaskOutput> putConfirmTask(String name, String sessionId) {
        return webClient.put()
                .uri(Constants.DRE_API_URL + "/ConfirmTask")
                .header("x-dresessionid", sessionId)
                .body(Mono.just(new ConfirmTaskBody(name)), ConfirmTaskBody.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("putConfirmTask failed with error code: " + response.statusCode()));
                })
                .bodyToMono(ConfirmTaskOutput.class);
    }

    public Mono<EnactmentDeleteOutput> putEnactmentDelete(String enactmentid, String sessionId) {
        return webClient.put()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(Constants.PRS_API_URL + "/EnactmentDelete")
                                .queryParam("id", enactmentid)
                                .build())
                .header("x-dresessionid", sessionId)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return Mono.error(new IllegalStateException("putEnactmentDelete failed with error code: " + response.statusCode()));
                })
                .bodyToMono(EnactmentDeleteOutput.class);
    }

}
