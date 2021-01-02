package com.capable.physiciandss.schedulers;

import ca.uhn.fhir.context.FhirContext;
import com.capable.physiciandss.model.deontics.get.Enactment;
import com.capable.physiciandss.model.deontics.get.ItemData;
import com.capable.physiciandss.model.deontics.get.PlanTask;
import com.capable.physiciandss.services.DeonticsRequestService;
import com.capable.physiciandss.services.HapiRequestService;
import com.capable.physiciandss.utils.Constants;
import com.capable.physiciandss.utils.OntologyCodingHandlingDeontics;
import com.capable.physiciandss.utils.ReferenceHandling;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.capable.physiciandss.utils.Constants.*;

@Component
public class ScheduledTasks {

    private static final Logger log =
            LoggerFactory.getLogger(ScheduledTasks.class);
    private final HapiRequestService hapiRequestService = new HapiRequestService();
    private final DeonticsRequestService deonticsRequestService = new DeonticsRequestService();


    @Scheduled(fixedRate = 10000)
    @Async
    public void checkForDataToProcess() {
        log.info(Constants.SCHEDULER_TASK_INFO);
        ArrayList<Communication> communicationList = (ArrayList<Communication>) hapiRequestService
                .getCommunicationList(Communication.CommunicationStatus.INPROGRESS);
        if (!communicationList.isEmpty()) {
            communicationList.forEach(
                    communication -> hapiRequestService
                            .updateCommunication(
                                    communication, Communication.CommunicationStatus.COMPLETED
                            ));

            Set<String> alreadyProcessedPatients = new HashSet<>();
            communicationList.forEach(communication ->
            {
                if (!communication.getPayload().isEmpty()) {
                    String payloadType = communication.getPayload().get(0).getContentReference().getType();
                    Reference payloadResourceReference = communication.getPayload().get(0).getContentReference();
                    Optional<String> patientId;
                    switch (payloadType) {
                        case "MedicationRequest":
                            patientId = Optional.ofNullable(hapiRequestService.
                                    getMedicationRequest(payloadResourceReference.getReference()).
                                    getSubject().
                                    getReference());
                            log.debug("Found change in Medication Request for patient with id: " + patientId);
                            if (patientId.isPresent() && !alreadyProcessedPatients.contains(patientId.get())) {
                                alreadyProcessedPatients.add(patientId.get());
                                patientId.ifPresent(this::handlePatient);
                            }
                            break;
                        case "Patient":
                            patientId = Optional.of(payloadResourceReference.getReference());
                            if (!alreadyProcessedPatients.contains(patientId.get())) {
                                log.debug("Found information about new Patient with id: " + patientId);
                                alreadyProcessedPatients.add(patientId.get());
                                patientId.ifPresent(this::handleNewPatient);
                            }
                            break;
                        default:
                            log.debug(SCHEDULER_TASK_BAD_PAYLOAD_TYPE);
                    }
                } else {
                    log.debug("Payload is missing");
                }
            });
        } else {
            log.debug("There isn't any data to process");
        }
    }

    private void handleNewPatient(String patientId) {
        deonticsRequestService
                .getPathwayByName(META_GUIDELINE_NAME)
                .subscribe(pathways -> {
                    if (pathways.length < 1) {
                        log.debug("Meta guideline is missing");
                    } else {
                        deonticsRequestService
                                .postEnact(META_GUIDELINE_NAME + ".pf", patientId)
                                .subscribe(postEnactResult -> deonticsRequestService
                                        .getEnactmentsByEnactmentId(postEnactResult.getEnactmentid())
                                        .subscribe(
                                                enactments -> handleEnactment(enactments[0], patientId), getEnactException -> {
                                                }), postEnactException -> {
                                });
                    }
                }, pathwayException -> {
                });
    }

    private void handlePatient(String patientId) {
        deonticsRequestService
                .getEnactmentsByPatientId(patientId)
                .subscribe(enactments -> {
                    if (enactments.length == 0) {
                        handleNewPatient(patientId);
                    } else {
                        for (Enactment enactment : enactments
                        ) {
                            handleEnactment(enactment, patientId);
                        }
                    }
                }, enactmentException -> {
                });
    }

    private void handleEnactment(Enactment enactment, String patientId) {
        deonticsRequestService
                .getConnect(enactment.getId())
                .subscribe(
                        connect -> handleTasks(
                                enactment.getId(), patientId, connect.getDresessionid(), Optional.empty()
                        ), connectionException -> {
                        });
    }

    private void handleTasks(String enactmentId, String patientId,
                             String dreSessionId, Optional<PlanTask[]> alreadyProcessedTasks) {
        deonticsRequestService
                .getPlanTasks(DEONTICS_IN_PROGRESS_STATUS, dreSessionId)
                .subscribe(tasks -> {
                    if (tasks.length == 0) {
                        deonticsRequestService
                                .putEnactmentDelete(enactmentId, dreSessionId)
                                .subscribe(
                                        enactmentDeleteOutput -> {
                                            if (enactmentDeleteOutput.getDeleted().equals("true")) {
                                                log.debug("Enactment was deleted");
                                            } else {
                                                log.debug("Deletion of Enactment was unsuccessful");
                                            }
                                        }, enactmentDeleteException -> {
                                        });
                    } else {
                        for (PlanTask task : tasks) {
                            if (!alreadyProcessedTasks.isPresent() || Arrays.stream(alreadyProcessedTasks.get()).
                                    noneMatch(planTask -> planTask.getName().equals(task.getName())))
                                handleTask(enactmentId, patientId, tasks, dreSessionId, task);
                        }
                    }
                }, planTasksException -> {
                });
    }

    private void handleTask(String enactmentId, String patientId, PlanTask[] tasks, String dreSessionId, PlanTask task) {
        switch (task.getType()) {
            case DEONTICS_ENQUIRY_TASK_TYPE:
                log.debug("Found " + DEONTICS_ENQUIRY_TASK_TYPE + " task to process");
                handleEnquiryTask(enactmentId, task, tasks, dreSessionId, patientId);
                break;
            case DEONTICS_ACTION_TASK_TYPE:
                log.debug("Found " + DEONTICS_ACTION_TASK_TYPE + " task to process");
                handleActionTask(enactmentId, task, tasks, dreSessionId, patientId);
                break;
            default:
                log.info(SCHEDULER_TASK_BAD_DEONTIC_TASKS_TYPE);
                break;
        }
    }

    private void handleEnquiryTask(String enactmentId, PlanTask task, PlanTask[] tasks, String dreSessionId, String patientId) {
        deonticsRequestService
                .getData(task.getName(), dreSessionId)
                .subscribe(itemDataList -> {
                    HashMap<String, String> dataItemToValueMap = new HashMap<>();
                    for (ItemData itemData : itemDataList) {
                        Optional<String> value;
                        JsonNode metaProperties = itemData.getMetaprops();
                        if (metaProperties.findValue("source") != null) {
                            switch (metaProperties.get("source").asText()) {
                                case "stored":
                                    log.debug("Found stored data item to process");
                                    value = handleStoredData(enactmentId, task, itemData, dreSessionId, patientId);
                                    break;
                                case "abstracted":
                                    log.debug("Found abstracted data item to process");
                                    value = handleAbstractedData(enactmentId, task, itemData, dreSessionId, patientId);
                                    break;
                                case "reported":
                                    log.debug("Found reported data item to process");
                                    value = handleReportedData(enactmentId, task, itemData, dreSessionId, patientId);
                                    break;
                                default:
                                    log.debug("Unknown source type  in enquiry task");
                                    value = Optional.empty();
                                    break;
                            }
                            value.ifPresent(s -> dataItemToValueMap.put(itemData.getName(), s));
                        } else {
                            log.debug("Missing source node in enquiry task");
                        }
                    }
                    deonticsRequestService.putDataValues(dataItemToValueMap, dreSessionId).subscribe(
                            dataValuesOutput -> {
                                tryToFinishTask(enactmentId, task, tasks, dreSessionId, patientId);
                            }
                    );
                }, getDataException -> {
                });
    }

    private Optional<String> handleReportedData(String enactmentId, PlanTask currentProcessedTask, ItemData itemData,
                                                String dreSessionId, String patientId) {
        JsonNode metaProperties = itemData.getMetaprops();
        if (metaProperties.findValue("ontology.coding") != null) {
            String ontologyCodingDeon = metaProperties.get("ontology.coding").asText();
            OntologyCodingHandlingDeontics ontologyCoding = new OntologyCodingHandlingDeontics(ontologyCodingDeon);
            boolean ifTaskAlreadyExists = false;
            ArrayList<Task> tasks = (ArrayList<Task>) hapiRequestService.getTaskList(Task.TaskStatus.REQUESTED);
            for (Task task : tasks) {
                if (task.getFor().getReference().equals(patientId)) {
                    if (task.getFocus().getType().equals("Observation")) {
                        Observation observation = hapiRequestService
                                .getObservation(task.getFocus().getReference());
                        Coding observationCoding = observation.getCode().getCodingFirstRep();
                        Coding ontologyCoding_ = ontologyCoding.getCoding();
                        if (isCodingMatching(
                                observationCoding.getCode(), ontologyCoding_.getCode(),
                                observationCoding.getSystem(), ontologyCoding_.getSystem()
                        )) {
                            log.debug("Task with given code already exist in handling reported data\"");
                            ifTaskAlreadyExists = true;
                            if (observation.getStatus().equals(Observation.ObservationStatus.REGISTERED)) {
                                log.debug("Observation affiliated with task has been filled in handling reported data\"");
                                hapiRequestService.updateTask(task, Task.TaskStatus.COMPLETED);
                                return Optional.of(observation.getValueQuantity().getValue().toPlainString());
                            }
                            break;
                        }
                    }
                }
            }
            if (!ifTaskAlreadyExists) {
                log.debug("Task with given code doesnt exist in handling reported data\"");
                String observationId = prepareRequestedObservation(ontologyCoding.getCoding());
                prepareRequestedTask(patientId, observationId);
                hapiRequestService
                        .createCommunication(Communication.CommunicationStatus.PREPARATION, observationId);
                log.debug("Put communication resource with reference at medication request in HAPI FHIR");
            }
        } else {
            log.debug("Missing ontology.coding in metaProperties in handling reported data");
        }
        return Optional.empty();
    }

    private boolean isCodingMatching(String code, String code2, String system, String system2) {
        if (code == null || code2 == null || system == null || system2 == null)
            return false;
        return code
                .equals(code2)
                && system
                .equals(system2);
    }

    private void prepareRequestedTask(String patientId, String focusResourceId) {
        hapiRequestService.createTask(new ReferenceHandling(patientId).getReference(), new ReferenceHandling(focusResourceId).getReference());
    }

    private String prepareRequestedObservation(Coding coding) {
        return hapiRequestService.createObservation(coding.getSystem(), coding.getCode(), Observation.ObservationStatus.PRELIMINARY);
    }


    private Optional<String> handleAbstractedData(String enactmentId, PlanTask task, ItemData itemData, String dreSessionId, String patientId) {
        JsonNode metaProperties = itemData.getMetaprops();
        if (metaProperties.findValue("ontology.coding") != null) {
            String ontologyCoding = metaProperties.get("ontology.coding").asText();
            OntologyCodingHandlingDeontics codingHandling = new OntologyCodingHandlingDeontics(ontologyCoding);
            String itemDataValue = "0";
            DateTimeType yesterdayDate = getDateBeforeCurrentDate(1);
            DateTimeType twoDaysAgoDate = getDateBeforeCurrentDate(2);
            DateTimeType threeDaysAgoDate = getDateBeforeCurrentDate(3);
            switch (codingHandling.getSystem()) {
                case SNOMED_CODING_HAPI:
                    switch (codingHandling.getCode()) {
                        case IMMUNOTHERAPY_CODE:
                            log.debug("Found immunotherapy case for patient with id: " + patientId);
                            return handleOnImmunotherapy(enactmentId, task, itemData, dreSessionId, patientId, itemDataValue);
                        case COMPLICATED_DIARRHEA_CODE:
                            log.debug("Found complicated diarrhea case for patient with id: " + patientId);
                            return handleComplicatedDiarrhea(enactmentId, task, itemData, dreSessionId, patientId, itemDataValue,
                                    yesterdayDate);
                        case PERSISTENT_DIARRHEA_CODE:
                            log.debug("Found persistent diarrhea case for patient with id: " + patientId);
                            return handlePersistentDiarrhea(enactmentId, task, itemData, dreSessionId, patientId, itemDataValue,
                                    yesterdayDate, twoDaysAgoDate, threeDaysAgoDate);
                        default:
                            log.debug("Unknown code value in handling abstracted data");
                            break;
                    }
                    break;
                default:
                    log.debug("Unknown coding system in handling abstracted data");
                    break;
            }
        } else {
            log.debug("Missing ontology.coding in metaProperties in handling abstracted data");
        }
        return Optional.empty();
    }

    private DateTimeType getDateBeforeCurrentDate(int daysCount) {
        DateTimeType yesterdayDate = new DateTimeType(new Date());
        yesterdayDate.add(5, -daysCount);
        return yesterdayDate;
    }

    private Optional<String> handlePersistentDiarrhea(String enactmentId, PlanTask task, ItemData itemData,
                                                      String dreSessionId, String patientId, String itemDataValue,
                                                      DateTimeType yesterdayDate, DateTimeType twoDaysAgo,
                                                      DateTimeType threeDaysAgo) {
        boolean ifCurrentDay = false;
        boolean ifYesterday = false;
        boolean ifTwoDaysAgo = false;
        ArrayList<Observation> observationList = (ArrayList<Observation>) hapiRequestService.
                getObservationList(patientId, SNOMED_CODING_HAPI, DIARRHEA_SYMPTOMS_CODE);
        for (Observation observation : observationList) {
            DateTimeType observationDate = observation.getEffectiveDateTimeType();
            if (!ifCurrentDay
                    && observationDate.after(yesterdayDate)) {
                ifCurrentDay = true;
            } else if (!ifYesterday
                    && isBetweenDates(observationDate, yesterdayDate, twoDaysAgo)) {
                ifYesterday = true;
            } else if (!ifTwoDaysAgo
                    && isBetweenDates(observationDate, twoDaysAgo, threeDaysAgo)) {
                ifTwoDaysAgo = true;
            }
            if (ifCurrentDay && ifTwoDaysAgo && ifYesterday) {
                itemDataValue = "1";
                break;
            }
        }
        return Optional.of(itemDataValue);
    }

    private boolean isBetweenDates(DateTimeType Date, DateTimeType firstDate, DateTimeType secondDate) {
        if (firstDate.after(secondDate)) {
            return Date.before(firstDate)
                    && Date.after(secondDate);
        } else {
            return Date.before(secondDate)
                    && Date.after(firstDate);
        }
    }

    private Optional<String> handleComplicatedDiarrhea(String enactmentId, PlanTask task, ItemData itemData,
                                                       String dreSessionId, String patientId, String itemDataValue, DateTimeType yesterdayDate) {
        ArrayList<Observation> observations = (ArrayList<Observation>) hapiRequestService.
                getObservationList(patientId, SNOMED_CODING_HAPI, STRONG_DIARRHEA_SYMPTOMS_CODE);
        observations.addAll(hapiRequestService.
                getObservationList(patientId, SNOMED_CODING_HAPI, DIARRHEA_SYMPTOMS_CODE));
        for (Observation observation : observations) {
            if (observation.getEffectiveDateTimeType().after(yesterdayDate)) {
                itemDataValue = "1";
                break;
            }
        }
        return Optional.of(itemDataValue);

    }

    private Optional<String> handleOnImmunotherapy(String enactmentId, PlanTask task, ItemData itemData,
                                                   String dreSessionId, String patientId, String itemDataValue) {
        ArrayList<MedicationRequest> medicationRequests = (ArrayList<MedicationRequest>) hapiRequestService
                .getMedicationRequestList(patientId, SNOMED_CODING_HAPI, SUNITIB_CODE, MedicationRequest.MedicationRequestStatus.ACTIVE);
        medicationRequests.addAll(hapiRequestService
                .getMedicationRequestList(patientId, SNOMED_CODING_HAPI, NIVOLUMAB_CODE, MedicationRequest.MedicationRequestStatus.ACTIVE));
        if (filterMedicationRequestListByDate(medicationRequests, new DateTimeType(new Date())).isPresent())
            itemDataValue = "1";
        return Optional.of(itemDataValue);

    }


    private Optional<String> handleStoredData(String enactmentId, PlanTask task, ItemData itemData,
                                              String dreSessionId, String patientId) {
        JsonNode metaProperties = itemData.getMetaprops();
        if (metaProperties.findValue("resourceType") != null) {
            if (metaProperties.findValue("ontology.coding") != null) {
                String ontologyCoding = metaProperties.get("ontology.coding").asText();
                OntologyCodingHandlingDeontics codingHandling = new OntologyCodingHandlingDeontics(ontologyCoding);
                switch (metaProperties.get("resourceType").asText()) {
                    case "Observation":
                        log.debug("Checking observation for essential data - patient id: " + patientId);
                        return handleStoredObservationData(enactmentId, task, itemData, patientId, dreSessionId,
                                codingHandling.getSystem(), codingHandling.getCode());
                    case "MedicationRequest":
                        log.debug("Checking medication request for essential data  - patient id: " + patientId);
                        return handleStoredMedicationRequestData(enactmentId, task, itemData, patientId, dreSessionId,
                                codingHandling.getSystem(), codingHandling.getCode());
                }
            } else {
                log.debug("Missing ontology.coding in metaProperties in handling stored data");
            }
        } else {
            log.debug("Missing resourceType in metaProperties in handling stored data");
        }
        return Optional.empty();
    }


    private Optional<String> handleStoredMedicationRequestData(String enactmentId, PlanTask task, ItemData itemData,
                                                               String patientId, String dreSessionId, String system, String code) {
        ArrayList<MedicationRequest> medicationRequests = (ArrayList<MedicationRequest>) hapiRequestService.
                getMedicationRequestList(patientId, system, code, MedicationRequest.MedicationRequestStatus.ACTIVE);
        Optional<MedicationRequest> medicationRequest =
                filterMedicationRequestListByDate(medicationRequests, new DateTimeType(new Date()));
        String value = "0";
        if (medicationRequest.isPresent()) {
            value = "1";
        }
        return Optional.of(value);

    }


    private Optional<String> handleStoredObservationData(String enactmentId, PlanTask task, ItemData itemData,
                                                         String patientId, String dreSessionId, String system, String code) {
        ArrayList<Observation> observations = (ArrayList<Observation>) hapiRequestService.
                getObservationList(patientId, system, code);
        Optional<Observation> observation = filterObservationListByDate(observations, new DateTimeType(new Date()));
        String valueQuantity = "";
        if (observation.isPresent()) {
            valueQuantity = observation.get().getValueQuantity().getValue().toPlainString();
        }
        return Optional.of(valueQuantity);
    }

    private void handleActionTask(String enactmentId, PlanTask task, PlanTask[] tasks, String dreSessionId, String patientId) {
        JsonNode metaProperties = task.getMetaprops();
        if (metaProperties.findValue("interactive") != null) {
            switch (metaProperties.get("interactive").asText()) {
                case "0":
                    log.debug("Found automatic task to process for patient with id: " + patientId);
                    handleAutomaticTask(enactmentId, task, tasks, task.getProcedure(), patientId, dreSessionId);
                    break;
                case "1":
                    log.debug("Found interactive task to process for patient with id: " + patientId);
                    handleInteractiveTask(enactmentId, task, tasks, patientId, dreSessionId);
                    break;
                default:
                    log.debug("Wrong interactive value  in Action task");
                    break;
            }
        } else {
            log.debug("Missing interactive node in Action task");
        }
    }


    private void handleInteractiveTask(String enactmentId, PlanTask task, PlanTask[] tasks, String patientId, String dreSessionId) {
        JsonNode metaProperties = task.getMetaprops();
        if (metaProperties.findValue("resourceType") != null) {
            switch (metaProperties.get("resourceType").asText()) {
                case "MedicationRequest":
                    log.debug("Found interactiveMedicationRequest task: " + patientId);
                    handleInteractiveMedicationRequest(enactmentId, task, tasks, patientId, dreSessionId, metaProperties);
                    break;
                default:
                    log.debug("Wrong resourceType value in interactive task");
            }
        }
    }

    private void handleInteractiveMedicationRequest(String enactmentId, PlanTask currentProcessedTask, PlanTask[] currentlyProcessedTasks, String patientId,
                                                    String dreSessionId, JsonNode metaProperties) {
        if (metaProperties.findValue("resource") != null) {
            MedicationRequest medicationRequest = FhirContext.forR4().newJsonParser()
                    .parseResource(MedicationRequest.class, metaProperties.get("resource").asText());
            boolean ifTaskAlreadyExists = false;
            ArrayList<Task> tasks = (ArrayList<Task>) hapiRequestService.getTaskList(Task.TaskStatus.REQUESTED);
            for (Task task : tasks) {
                if (task.getFor().getReference().equals(patientId)) {
                    if (task.getFocus().getType().equals("MedicationRequest")) {
                        MedicationRequest mR = hapiRequestService.
                                getMedicationRequest(task.getFocus().getReference());
                        Coding taskMrCoding = mR.getMedicationCodeableConcept().getCodingFirstRep();
                        Coding mRCoding = medicationRequest.getMedicationCodeableConcept().getCodingFirstRep();
                        if (isCodingMatching(taskMrCoding.getCode(), mRCoding.getCode(),
                                mRCoding.getSystem(), taskMrCoding.getSystem())) {
                            log.debug("Task with given code already exist in InteractiveMedicationRequest Task");
                            ifTaskAlreadyExists = true;
                            if (mR.getStatus()
                                    .equals(MedicationRequest.MedicationRequestStatus.ACTIVE)) {
                                log.debug("Medication request affiliated with task has been activated in InteractiveMedicationRequest Task");
                                hapiRequestService.updateTask(task, Task.TaskStatus.COMPLETED);
                                tryToFinishTask(enactmentId, currentProcessedTask, currentlyProcessedTasks, dreSessionId, patientId);
                            }
                            break;
                        }
                    }
                }
            }
            if (!ifTaskAlreadyExists) {
                log.debug("Task with given code doesnt exist in InteractiveMedicationRequest Task");
                String medicationRequestId = hapiRequestService
                        .createMedicationRequest(medicationRequest, MedicationRequest.MedicationRequestStatus.DRAFT,
                                MedicationRequest.MedicationRequestIntent.PROPOSAL, patientId);
                prepareRequestedTask(patientId, medicationRequestId);
                hapiRequestService
                        .createCommunication(Communication.CommunicationStatus.PREPARATION, medicationRequestId);
                log.debug("Put communication resource with reference at medication request in HAPI FHIR");
            }
        } else {
            log.debug("Missing resource Node in InteractiveMedicationRequest Task");
        }
    }


    private void handleAutomaticTask(String enactmentId, PlanTask task, PlanTask[] tasks, String procedure, String patientId, String dreSessionId) {
        deonticsRequestService
                .postEnact(procedure + ".pf", patientId)
                .subscribe(postEnactResult ->
                        deonticsRequestService
                                .getEnactmentsByEnactmentId(postEnactResult.getEnactmentid())
                                .subscribe(enactments -> {
                                    log.debug("Started new enact for currently processed patient with id: " +
                                            patientId + " new pathway: " + procedure);
                                    handleEnactment(enactments[0], patientId);
                                    tryToFinishTask(enactmentId, task, tasks, dreSessionId, patientId);
                                }, getEnactException -> {
                                }), postEnactException -> {
                });
    }

    private void tryToFinishTask(String enactmentId, PlanTask planTask, PlanTask[] currentlyProcessedTasks, String dreSessionId, String patientId) {
        deonticsRequestService
                .getQueryConfirmTask(planTask.getName(), dreSessionId)
                .subscribe(queryConfirmTask -> {
                    if (queryConfirmTask.getPrecondition() == null || queryConfirmTask.getCauses() == null) {
                        deonticsRequestService
                                .putConfirmTask(planTask.getName(), dreSessionId)
                                .subscribe(confirmTaskOutput -> {
                                    if (!confirmTaskOutput.getState().equals("completed")) {
                                        log.debug("Cannot complete task");
                                    } else {
                                        log.debug("Task has been completed for patient with id: " + patientId);
                                        deonticsRequestService
                                                .getPlanTasks(DEONTICS_IN_PROGRESS_STATUS, dreSessionId)
                                                .subscribe(tasks -> handleTasks(enactmentId, patientId, dreSessionId, Optional.of(currentlyProcessedTasks)));
                                    }
                                }, confirmTaskException -> {
                                });
                    } else {
                        log.debug("Cannot finish task,\nReasons:" + queryConfirmTask.toString());
                    }
                }, queryConfirmTaskException -> {
                });
    }

    private Optional<Observation> filterObservationListByDate
            (ArrayList<Observation> observations, DateTimeType date) {
        if (observations.size() == 0)
            return Optional.empty();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HAPI_DATETIMETYPE_FORMAT_OBS);
        observations.sort(Comparator.comparing(o ->
                LocalDate.parse(o.getEffectiveDateTimeType().asStringValue(), formatter)));
        if (observations.get(observations.size() - 1).getEffectiveDateTimeType().before(date)) {
            return Optional.of(observations.get(0));
        } else {
            return Optional.empty();
        }
    }

    private Optional<MedicationRequest> filterMedicationRequestListByDate
            (ArrayList<MedicationRequest> medicationRequests, DateTimeType date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HAPI_DATETIMETYPE_FORMAT_MR);
        if (medicationRequests.size() == 0)
            return Optional.empty();
        medicationRequests.sort(Comparator.comparing(mR -> LocalDate.parse(mR.getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod()
                .getEndElement().asStringValue(), formatter)));

        DateTimeType dosageStartDate = medicationRequests.get(medicationRequests.size() - 1).getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod().getStartElement();

        DateTimeType dosageEndDate = medicationRequests.get(medicationRequests.size() - 1).getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod().getEndElement();
        if (isBetweenDates(date, dosageStartDate, dosageEndDate)) {
            return Optional.of(medicationRequests.get(0));
        } else {
            return Optional.empty();
        }
    }


}
