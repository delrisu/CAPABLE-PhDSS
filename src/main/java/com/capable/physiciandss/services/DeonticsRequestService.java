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

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Klasa zapewniająca komunikację z komponentem Deontics Engine
 */
@Service
public class DeonticsRequestService extends RootService {

    WebClient webClient;


    /**
     * Konfiguruje webClient oraz inicjalizuje loggera
     */
    public DeonticsRequestService() {
        log = LoggerFactory.getLogger(this.getClass().getName());
        ApplicationContext context = new AnnotationConfigApplicationContext(WebClientConfig.class);
        webClient = context.getBean("webClient", WebClient.class);
        log.info("DeonticsRequestService has been created");
    }

    /**
     * @return Lista wszystkich Enactment
     */
    public Mono<Enactment[]> getEnactments() {
        return webClient.get()
                .uri(Constants.PRS_API_URL + "/Enactments")
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "getEnactments"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getEnactments"))
                .bodyToMono(Enactment[].class);
    }

    /**
     * @param patientId Identyfikator pacjenta
     * @return Lista wszystich Enactment związanych z pacjentem o zadanym identyfikataorze
     */
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

    /**
     * @param enactmentId Identyfikator Enactment
     * @return Enactment o zadanym Identyfikatorze
     */
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

    /**
     * @param temp Wartość logiczna określająca czy Pathway może być tymczasowy
     * @return Listę Pathway'ów spełniających kryteria
     */
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

    /**
     * @param name Nazwa Pathway'a, którego chcemy uzyskać
     * @return Pathway o zadanej nazwie
     */
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


    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param enquiryName Nazwa zadania typu pytanie, którego elementy danych chcemy odczytać
     * @param sessionId   Identyfikator sesji
     * @return Lista elementów danych powiązanych z określonym zadaniem
     */
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

    /**
     * Metoda służąca do połączenia się do enactment i utworzenia sesji
     *
     * @param enactmentId Identyfikator Enactment z którym chcemy się połączyć
     * @return Obiekt mający między innymi Identyfikator sesji wymagany, aby używać większości end-pointów DREapi
     */
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

    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param runtimeid Identyfikator zadania
     * @param sessionId Identyfikator sesji
     * @return Zadanie o zadanym identyfikatorze
     */
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

    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param state     Określa stan zadań
     * @param sessionId Identyfikator sesji
     * @return Listę zadań o określonym stanie
     */
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

    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param state     Określa stan zadań
     * @param taskName  Nazwa zadania
     * @param sessionId Identyfikator sesji
     * @return Listę zadań znajdujących się na niższym poziomie drzewa, niż zadanie o zadanej nazwie
     */
    public Mono<PlanTask[]> getPlanTasksUnderTask(String state, String taskName, String sessionId) {
        return webClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(Constants.DRE_API_URL + "/PlanTasks")
                                .queryParam("state", state)
                                .queryParam("name", taskName)
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

    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param taskName  Mazwa zadania
     * @param sessionId Identyfikator sesji
     * @return Obiekt zawierający informację czy zadanie o zadanej nazwie jest w stanie, który pozwala na jego potwierdzenie
     */
    public Mono<QueryConfirmTask> getQueryConfirmTask(String taskName, String sessionId) {
        return webClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(Constants.DRE_API_URL + "/QueryConfirmTask")
                                .queryParam("name", taskName)
                                .build())
                .header("x-dresessionid", sessionId)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "getQueryConfirmTask"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("getQueryConfirmTask"))
                .bodyToMono(QueryConfirmTask.class);
    }


    /**
     * Metoda tworzy Enactment z ścieżką określaną przez pathwayUri oraz z pacjentem wskazanym przez patientId
     *
     * @param pathwayuri uri Pathway'a znajdującego się w bazie danych Deontics
     * @param patientid  Identyfikator pacjenta
     * @return Obiekt zawierający informację identyfikatorze sesji oraz identyfikatorze utworzonego enactment
     */
    public Mono<EnactOutput> postEnact(String pathwayuri, String patientid) {
        return webClient.post()
                .uri(Constants.DRE_API_URL + "/Enact")
                .body(Mono.just(new EnactBody(pathwayuri, patientid)), EnactBody.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "postEnact"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("postEnact"))
                .bodyToMono(EnactOutput.class);
    }

    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param name      Nazwa elementu danych
     * @param value     Nowa wartość elementu danych
     * @param sessionId Identyfikator sesji
     * @return Obiekt zawierający informację między innymi o statusie operacji
     */
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

    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param dataItemNameValuesMap Mapa, która łączy dwójki imię elementu danych oraz jego nową wartość
     * @param sessionId             Identyfikator sesji
     * @return Listę zawierającą obiekty zawierające informację między innymi o statusie każdej z operacji
     */
    public Mono<DataValueOutput[]> putDataValues(HashMap<String, String> dataItemNameValuesMap, String sessionId) {
        DataValuesBody dataValuesBody = new DataValuesBody();
        ArrayList<DataValueBody> dataValueBodyArrayList = new ArrayList<>();
        dataItemNameValuesMap.forEach(
                (itemDataName, itemDataValue) -> {
                    log.info("Item Data name: " + itemDataName + " Item Data Value: " + itemDataValue);
                    dataValueBodyArrayList.add(new DataValueBody(itemDataName, itemDataValue));
                }
        );
        dataValuesBody.setDataValueBodies(dataValueBodyArrayList.toArray(new DataValueBody[0]));
        return webClient.put()
                .uri(Constants.DRE_API_URL + "/DataValue")
                .header("x-dresessionid", sessionId)
                .body(Mono.just(dataValuesBody.getDataValueBodies()), DataValueBody[].class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> onError(response, "putDataValue"))
                .onStatus(HttpStatus::is2xxSuccessful, response -> onSuccess("putDataValue"))
                .bodyToMono(DataValueOutput[].class);
    }

    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param name      Nazwa zadania
     * @param sessionId Identyfikator sesji
     * @return Obiekt zawierający informację o statusie operacji
     */
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

    /**
     * Do użycia tej metody wymagane jest wcześniejsze połączenie się do Enactment i utworzenie sesji
     *
     * @param enactmentid Identyfikator Enactment
     * @param sessionId   Identyfikator sesji
     * @return Obiekt zawierający informację o statusie operacji
     */
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
