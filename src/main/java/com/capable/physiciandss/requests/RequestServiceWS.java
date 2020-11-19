package com.capable.physiciandss.requests;


import com.capable.physiciandss.configuration.WebClientConfig;
import com.capable.physiciandss.model.get.*;
import com.capable.physiciandss.model.post.EnactBody;
import com.capable.physiciandss.model.post.EnactOutput;
import com.capable.physiciandss.model.put.*;
import com.capable.physiciandss.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;




@Service
public class RequestServiceWS {

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    WebClient webClient;


    public RequestServiceWS() {
        ApplicationContext context = new AnnotationConfigApplicationContext(WebClientConfig.class);
        webClient = context.getBean("webClient", WebClient.class);
        log.info("Utworzono RequestService");
    }

    public Mono<Enactment[]> getEnactments() {
        return webClient.get()
                .uri(Constants.PRS_API_URL + "/Enactments")
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    IllegalStateException ex = new IllegalStateException("getEnactments" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("getEnactments" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
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
                    IllegalStateException ex = new IllegalStateException("getPathway" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("getPathway" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
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
                    IllegalStateException ex = new IllegalStateException("getData" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("getData" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
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
                    IllegalStateException ex = new IllegalStateException("getConnect" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("getConnect" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
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
                    IllegalStateException ex = new IllegalStateException("getTask" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("getTask" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
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
                    IllegalStateException ex = new IllegalStateException("getPlanTasks" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("getPlanTasks" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
                })
                .bodyToMono(PlanTask[].class);
    }

    public Mono<EnactOutput> postEnact(String pathwayid, String patientid) {
        return webClient.post()
                .uri(Constants.DRE_API_URL + "/Enact")
                .body(Mono.just(new EnactBody(pathwayid, patientid)), EnactBody.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    IllegalStateException ex = new IllegalStateException("postEnact" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("postEnact" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
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
                    IllegalStateException ex = new IllegalStateException("putDataValue" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("putDataValue" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
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
                    IllegalStateException ex = new IllegalStateException("putConfirmTask" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("putConfirmTask" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
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
                    IllegalStateException ex = new IllegalStateException("putEnactmentDelete" + Constants.REQUEST_FAILED_MESSAGE
                            + response.statusCode());
                    log.debug(ex.getMessage());
                    return Mono.error(ex);
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    log.info("putEnactmentDelete" + Constants.REQUEST_SUCCEDED_MESSAGE);
                    return Mono.empty();
                })
                .bodyToMono(EnactmentDeleteOutput.class);
    }

}
