package com.capable.physiciandss.services;


import com.capable.physiciandss.configuration.WebClientConfig;
import com.capable.physiciandss.model.deontics.get.*;
import com.capable.physiciandss.model.deontics.post.EnactBody;
import com.capable.physiciandss.model.deontics.post.EnactOutput;
import com.capable.physiciandss.model.deontics.put.*;
import com.capable.physiciandss.utils.Constants;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
public class DeonticsRequestService extends RootService {

    WebClient webClient;


    public DeonticsRequestService() {
        log = LoggerFactory.getLogger(this.getClass().getName());
        ApplicationContext context = new AnnotationConfigApplicationContext(WebClientConfig.class);
        webClient = context.getBean("webClient", WebClient.class);
        log.info("DeonticsRequestService has been created");
    }

    public Mono<Enactment[]> getEnactments() {
        return webClient.get()
                .uri(Constants.PRS_API_URL + "/Enactments")
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "getEnactments"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getEnactments"))
                .bodyToMono(Enactment[].class);
    }

    public Mono<Enactment[]> getEnactmentsByPatientId(String patientId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.PRS_API_URL + "/EnactmentsExtended")
                        .queryParam("groupid", patientId)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "getEnactmentsByPatientId"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getEnactmentsByPatientId"))
                .bodyToMono(Enactment[].class);
    }

    public Mono<Enactment[]> getEnactmentsByEnactmentId(String enactmentId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.PRS_API_URL + "/EnactmentsExtended")
                        .queryParam("id", enactmentId)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "getEnactmentsByEnactmentId"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getEnactmentsByEnactmentId"))
                .bodyToMono(Enactment[].class);
    }

    public Mono<Pathway[]> getPathway(boolean temp) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.PRS_API_URL + "/Pathways")
                        .queryParam("temp", temp)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "getPathway"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getPathway"))
                .bodyToMono(Pathway[].class);
    }

    public Mono<Pathway[]> getPathwayByName(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.PRS_API_URL + "/Pathways")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "getPathwayByName"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getPathwayByName"))
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
                .onStatus(HttpStatus::isError, response -> onError(response, "getData"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getData"))
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
                .onStatus(HttpStatus::isError, response -> onError(response, "getConnect"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getConnect"))
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
                .onStatus(HttpStatus::isError, response -> onError(response, "getTask"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getTask"))
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
                .onStatus(HttpStatus::isError, response -> onError(response, "getPlanTasks"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getPlanTasks"))
                .bodyToMono(PlanTask[].class);
    }

    public Mono<QueryConfirmTask> getQueryConfirmTask(int runtimeid, String sessionId) {
        return webClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(Constants.DRE_API_URL + "/QueryConfirmTask")
                                .queryParam("runtimeid", runtimeid)
                                .build())
                .header("x-dresessionid", sessionId)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "getQueryConfirmTask"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getQueryConfirmTask"))
                .bodyToMono(QueryConfirmTask.class);
    }


    public Mono<EnactOutput> postEnact(String pathwayid, String patientid) {
        return webClient.post()
                .uri(Constants.DRE_API_URL + "/Enact")
                .body(Mono.just(new EnactBody(pathwayid, patientid)), EnactBody.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "postEnact"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("postEnact"))
                .bodyToMono(EnactOutput.class);
    }

    public Mono<DataValueOutput> putDataValue(String name, String value, String sessionId) {
        return webClient.put()
                .uri(Constants.DRE_API_URL + "/DataValue")
                .header("x-dresessionid", sessionId)
                .body(Mono.just(new DataValueBody(name, value)), DataValueBody.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "putDataValue"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("putDataValue"))
                .bodyToMono(DataValueOutput.class);
    }

    public Mono<ConfirmTaskOutput> putConfirmTask(String name, String sessionId) {
        return webClient.put()
                .uri(Constants.DRE_API_URL + "/ConfirmTask")
                .header("x-dresessionid", sessionId)
                .body(Mono.just(new ConfirmTaskBody(name)), ConfirmTaskBody.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "putConfirmTask"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("putConfirmTask"))
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
                .onStatus(HttpStatus::isError, response -> onError(response, "putEnactmentDelete"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("putEnactmentDelete"))
                .bodyToMono(EnactmentDeleteOutput.class);
    }

}
