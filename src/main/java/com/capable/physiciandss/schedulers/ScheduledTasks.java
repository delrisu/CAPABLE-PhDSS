package com.capable.physiciandss.schedulers;

import com.capable.physiciandss.model.deontics.get.Enactment;
import com.capable.physiciandss.model.deontics.get.ItemData;
import com.capable.physiciandss.model.deontics.get.PlanTask;
import com.capable.physiciandss.services.DeonticsRequestService;
import com.capable.physiciandss.services.HapiRequestService;
import com.capable.physiciandss.utils.Constants;
import com.capable.physiciandss.utils.OntologyCodingHandlingDeontics;
import com.capable.physiciandss.utils.ReferenceHandling;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
                                    getMedicationRequestById(payloadResourceReference.getReference()).
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
                            if (patientId.isPresent() && !alreadyProcessedPatients.contains(patientId.get())) {
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
                        log.info("Meta guideline is missing");
                    } else {
                        deonticsRequestService
                                .postEnact(pathways[0].getId(), patientId)
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
                             String dreSessionId, Optional<PlanTask[]> tasksToProcess) {
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
                        if (tasksToProcess.isPresent()) {
                            for (PlanTask task : tasksToProcess.get()) {
                                handleTask(enactmentId, patientId, dreSessionId, task);
                            }
                        } else {
                            for (PlanTask task : tasks) {
                                handleTask(enactmentId, patientId, dreSessionId, task);

                            }
                        }
                    }
                }, planTasksException -> {
                });
    }

    private void handleTask(String enactmentId, String patientId, String dreSessionId, PlanTask task) {
        switch (task.getType()) {
            case DEONTICS_ENQUIRY_TASK_TYPE:
                log.debug("Found " + DEONTICS_ENQUIRY_TASK_TYPE + " task to process");
                handleEnquiryTask(enactmentId, task, dreSessionId, patientId);
                break;
            case DEONTICS_ACTION_TASK_TYPE:
                log.debug("Found " + DEONTICS_ACTION_TASK_TYPE + " task to process");
                handleActionTask(enactmentId, task, dreSessionId, patientId);
                break;
            default:
                log.info(SCHEDULER_TASK_BAD_DEONTIC_TASKS_TYPE);
                break;
        }
    }

    private void handleEnquiryTask(String enactmentId, PlanTask task, String dreSessionId, String patientId) {
        deonticsRequestService
                .getData(task.getName(), dreSessionId)
                .subscribe(itemDataList -> {
                    for (ItemData itemData : itemDataList) {
                        JsonNode metaProperties = itemData.getMetaprops();
                        if (!metaProperties.findValue("source").isNull()) {
                            switch (metaProperties.get("source").asText()) {
                                case "stored":
                                    log.debug("Found stored data item to process");
                                    handleStoredData(enactmentId, task, itemData, dreSessionId, patientId);
                                    break;
                                case "abstracted":
                                    log.debug("Found abstracted data item to process");
                                    handleAbstractedData(enactmentId, task, itemData, dreSessionId, patientId);
                                    break;
                                case "reported":
                                    log.debug("Found reported data item to process");
                                    handleReportedData(enactmentId, task, itemData, dreSessionId, patientId);
                                    break;
                                default:
                                    log.debug("Unknown source type");
                                    break;
                            }
                        } else {
                            log.debug("Missing source node");
                        }
                    }
                }, getDataException -> {
                });
    }

    //TODO fill gaps
    private void handleReportedData(String enactmentId, PlanTask task, ItemData itemData,
                                    String dreSessionId, String patientId) {
        JsonNode metaProperties = itemData.getMetaprops();
        if (!metaProperties.findValue("resourceType").isNull()) {
            if (!metaProperties.findValue("ontology.coding").isNull()) {
                String ontologyCodingDeon = metaProperties.get("ontology.coding").asText();
                OntologyCodingHandlingDeontics ontologyCoding = new OntologyCodingHandlingDeontics(ontologyCodingDeon);
                boolean ifTaskAlreadyExists = false;
                ArrayList<Task> tasks = null;//hapiRequestService.getTasks(Task.TaskStatus.REQUESTED);
                for (Task t : tasks) {
                    if (t.getFor().getReference().equals(patientId)) {
                        if (t.getFocus().getType().equals("Observation")) {
                            Observation observation = hapiRequestService
                                    .getObservation(t.getFocus().getReference());
                            if (observation.getCode().getCodingFirstRep().getCode()
                                    .equals(ontologyCoding.getCode())
                                    &&
                                    observation.getCode().getCodingFirstRep().getSystem()
                                            .equals(ontologyCoding.getSystem())) {
                                log.debug("Task with given code already exist");
                                ifTaskAlreadyExists = true;
                                if (observation.getStatus().equals(Observation.ObservationStatus.REGISTERED)) {
                                    log.debug("Observation affiliated with task has been filled");
                                    //hapiRequestService.putTask(Task.TaskStatus.COMPLETED);
                                    tryToFinishTask(enactmentId, task, dreSessionId, patientId);
                                }
                                break;
                            }
                        }
                    }
                }
                if (!ifTaskAlreadyExists) {
                    log.debug("Task with given code doesnt exist");
                    String observationId = prepareRequestedObservation(ontologyCoding.getCoding());
                    prepareRequestedTask(patientId, observationId);
                    hapiRequestService
                            .createCommunication(Communication.CommunicationStatus.PREPARATION, observationId);
                    log.debug("Put communication resource with reference at medication request in HAPI FHIR");
                }
            } else {
                log.debug("Missing ontology.coding in metaProperties");
            }
        } else {
            log.debug("Missing resourceType in metaProperties");
        }
    }

    //TODO fill gaps
    private void prepareRequestedTask(String patientId, String focusResourceId) {
        Task newTask = new Task();
        newTask.setIntent(Task.TaskIntent.ORDER);
        newTask.setStatus(Task.TaskStatus.REQUESTED);
        newTask.setFor(new ReferenceHandling(patientId).getReference());
        newTask.setFocus(new ReferenceHandling(focusResourceId).getReference());
        //hapiRequestService.createTask(newTask);
    }

    private String prepareRequestedObservation(Coding coding) {
        return hapiRequestService.createObservation(coding.getSystem(), coding.getCode(), Observation.ObservationStatus.PRELIMINARY);
    }


    private void handleAbstractedData(String enactmentId, PlanTask task, ItemData itemData, String dreSessionId, String patientId) {
        JsonNode metaProperties = itemData.getMetaprops();
        if (!metaProperties.findValue("ontology.coding").isNull()) {
            String ontologyCoding = metaProperties.get("ontology.coding").asText();
            OntologyCodingHandlingDeontics codingHandling = new OntologyCodingHandlingDeontics(ontologyCoding);
            String itemDataValue = "0";
            DateTimeType yesterdayDate = new DateTimeType(new Date());
            yesterdayDate.add(5, -1);
            DateTimeType twoDaysAgoDate = new DateTimeType(new Date());
            twoDaysAgoDate.add(5, -2);
            DateTimeType threeDaysAgoDate = new DateTimeType(new Date());
            threeDaysAgoDate.add(5, -3);
            switch (codingHandling.getSystem()) {
                case SNOMED_CODING_DEONTICS:
                    switch (codingHandling.getCode()) {
                        case IMMUNOTHERAPY_CODE:
                            handleOnImmunotherapy(enactmentId, task, itemData, dreSessionId, patientId, itemDataValue);
                            break;
                        case COMPLICATED_DIARRHEA_CODE:
                            handleComplicatedDiarrhea(enactmentId, task, itemData, dreSessionId, patientId, itemDataValue,
                                    yesterdayDate);
                            break;
                        case PERSISTENT_DIARRHEA_CODE:
                            handlePersistentDiarrhea(enactmentId, task, itemData, dreSessionId, patientId, itemDataValue,
                                    yesterdayDate, twoDaysAgoDate, threeDaysAgoDate);
                            break;
                        default:
                            log.debug("Unknown code value");
                            break;
                    }
                default:
                    log.debug("Unknown coding system");
                    break;
            }
        } else {
            log.debug("Missing ontology.coding in metaProperties");
        }
    }

    private void handlePersistentDiarrhea(String enactmentId, PlanTask task, ItemData itemData,
                                          String dreSessionId, String patientId, String itemDataValue,
                                          DateTimeType yesterdayDate, DateTimeType twoDaysAgo,
                                          DateTimeType threeDaysAgo) {
        boolean ifCurrentDay = false;
        boolean ifYesterday = false;
        boolean ifTwoDaysAgo = false;
        ArrayList<Observation> observationList = (ArrayList<Observation>) hapiRequestService.
                getObservationList(patientId);
        for (Observation observation : observationList) {
            Coding observationCoding = observation.getCode().getCodingFirstRep();
            if (observationCoding.getSystem().equals(SNOMED_CODING_HAPI) &&
                    observationCoding.getCode().equals(DIARRHEA_SYMPTOMS_CODE)) {
                if (!ifCurrentDay
                        && observation.getEffectiveDateTimeType().after(yesterdayDate)) {
                    ifCurrentDay = true;
                } else if (!ifYesterday
                        && observation.getEffectiveDateTimeType().before(yesterdayDate)
                        && observation.getEffectiveDateTimeType().after(twoDaysAgo)) {
                    ifYesterday = true;
                } else if (!ifTwoDaysAgo
                        && observation.getEffectiveDateTimeType().before(twoDaysAgo)
                        && observation.getEffectiveDateTimeType().after(threeDaysAgo)) {
                    ifTwoDaysAgo = true;
                }
            }
            if (ifCurrentDay && ifTwoDaysAgo && ifYesterday) {
                itemDataValue = "1";
                break;
            }
        }
        deonticsRequestService
                .putDataValue(itemData.getName(), itemDataValue, dreSessionId)
                .subscribe(dataValueOutput -> {
                    if (dataValueOutput.isSuccess()) {
                        log.debug("Put medication request data value in deontics  for patient with id: "
                                + patientId);
                        tryToFinishTask(enactmentId, task, dreSessionId, patientId);
                    } else {
                        log.debug("Cannot put dataValue");
                    }
                });
    }

    private void handleComplicatedDiarrhea(String enactmentId, PlanTask task, ItemData itemData,
                                           String dreSessionId, String patientId, String itemDataValue, DateTimeType yesterdayDate) {
        ArrayList<Observation> observations = (ArrayList<Observation>) hapiRequestService.getObservationList(patientId);
        for (Observation observation : observations) {
            Coding observationCoding = observation.getCode().getCodingFirstRep();
            if (observation.getEffectiveDateTimeType().after(yesterdayDate)) {
                if (observationCoding.getSystem().equals(SNOMED_CODING_HAPI) &&
                        (observationCoding.getCode().equals(STRONG_DIARRHEA_SYMPTOMS_CODE)
                                || observationCoding.getCode().equals(DIARRHEA_SYMPTOMS_CODE))) {
                    itemDataValue = "1";
                }
            }
        }
        deonticsRequestService
                .putDataValue(itemData.getName(), itemDataValue, dreSessionId)
                .subscribe(dataValueOutput -> {
                    if (dataValueOutput.isSuccess()) {
                        log.debug("Put medication request data value in deontics  for patient with id: "
                                + patientId);
                        tryToFinishTask(enactmentId, task, dreSessionId, patientId);
                    } else {
                        log.debug("Cannot put dataValue");
                    }
                });
    }

    private void handleOnImmunotherapy(String enactmentId, PlanTask task, ItemData itemData,
                                       String dreSessionId, String patientId, String itemDataValue) {
        ArrayList<MedicationRequest> medicationRequests = (ArrayList<MedicationRequest>) hapiRequestService.getMedicationRequestList(patientId);
        for (MedicationRequest medicationRequest : medicationRequests) {
            Coding mrCoding = medicationRequest.getCategoryFirstRep().getCodingFirstRep();
            if (mrCoding.getSystem().equals(SNOMED_CODING_HAPI) && mrCoding.getCode().equals(SUNITIB_CODE)) {
                itemDataValue = "1";
                break;
            } else if (mrCoding.getSystem().equals(SNOMED_CODING_HAPI) && mrCoding.getCode().equals(NIVOLUMAB_CODE)) {
                itemDataValue = "1";
                break;
            }
        }
        deonticsRequestService
                .putDataValue(itemData.getName(), itemDataValue, dreSessionId)
                .subscribe(dataValueOutput -> {
                    if (dataValueOutput.isSuccess()) {
                        log.debug("Put medication request data value in deontics  for patient with id: "
                                + patientId);
                        tryToFinishTask(enactmentId, task, dreSessionId, patientId);
                    } else {
                        log.debug("Cannot put dataValue");
                    }
                });
    }


    private void handleStoredData(String enactmentId, PlanTask task, ItemData itemData,
                                  String dreSessionId, String patientId) {
        JsonNode metaProperties = itemData.getMetaprops();
        if (!metaProperties.findValue("resourceType").isNull()) {
            if (!metaProperties.findValue("ontology.coding").isNull()) {
                String ontologyCoding = metaProperties.get("ontology.coding").asText();
                OntologyCodingHandlingDeontics codingHandling = new OntologyCodingHandlingDeontics(ontologyCoding);
                switch (metaProperties.get("resourceType").asText()) {
                    case "Observation":
                        log.debug("Checking observation for essential data - patient id: " + patientId);
                        handleStoredObservationData(enactmentId, task, itemData, patientId, dreSessionId,
                                codingHandling.getSystem(), codingHandling.getCode());
                        break;
                    case "MedicationRequest":
                        log.debug("Checking medication request for essential data  - patient id: " + patientId);
                        handleStoredMedicationRequestData(enactmentId, task, itemData, patientId, dreSessionId,
                                codingHandling.getSystem(), codingHandling.getCode());
                        break;
                }
            } else {
                log.debug("Missing ontology.coding in metaProperties");
            }
        } else {
            log.debug("Missing resourceType in metaProperties");
        }
    }


    private void handleStoredMedicationRequestData(String enactmentId, PlanTask task, ItemData itemData,
                                                   String patientId, String dreSessionId, String system, String code) {
        ArrayList<MedicationRequest> medicationRequests = (ArrayList<MedicationRequest>) hapiRequestService.
                getMedicationRequestList(patientId, system, code, MedicationRequest.MedicationRequestStatus.ACTIVE);
        Optional<MedicationRequest> medicationRequest =
                filterMedicationRequestListByDate(medicationRequests, new DateTimeType(new Date()));
        String value = "0";
        if (medicationRequest.isPresent()) {
            value = "1";
        }
        String finalValue = value;
        deonticsRequestService
                .putDataValue(itemData.getName(), value, dreSessionId)
                .subscribe(dataValueOutput -> {
                    if (dataValueOutput.isSuccess()) {
                        log.debug("Put medication request data value in deontics  for patient with id: "
                                + patientId + " dataValue: " + finalValue);
                        tryToFinishTask(enactmentId, task, dreSessionId, patientId);
                    } else {
                        log.debug("Cannot put dataValue");
                    }
                });
    }


    private void handleStoredObservationData(String enactmentId, PlanTask task, ItemData itemData,
                                             String patientId, String dreSessionId, String system, String code) {
        ArrayList<Observation> observations = (ArrayList<Observation>) hapiRequestService.
                getObservationList(patientId, system, code);
        Optional<Observation> observation = filterObservationListByDate(observations, new DateTimeType(new Date()));
        String valueQuantity = "";
        if (observation.isPresent()) {
            valueQuantity = observation.get().getValueQuantity().toString();
        }
        String finalValueQuantity = valueQuantity;
        deonticsRequestService
                .putDataValue(itemData.getName(), valueQuantity, dreSessionId)
                .subscribe(dataValueOutput -> {
                    if (dataValueOutput.isSuccess()) {
                        log.debug("Put observation data value in deontics  for patient with id: " +
                                patientId + " dataValue: " + finalValueQuantity);
                        tryToFinishTask(enactmentId, task, dreSessionId, patientId);
                    } else {
                        log.debug("Cannot put dataValue");
                    }
                });
    }

    private void handleActionTask(String enactmentId, PlanTask task, String dreSessionId, String patientId) {
        JsonNode metaProperties = task.getMetaprops();
        if (!metaProperties.findValue("interactive").isNull()) {
            switch (metaProperties.get("interactive").asText()) {
                case "0":
                    log.debug("Found automatic task to process for patient with id: " + patientId);
                    handleAutomaticTask(enactmentId, task, task.getProcedure(), patientId, dreSessionId);
                    break;
                case "1":
                    log.debug("Found interactive task to process for patient with id: " + patientId);
                    handleInteractiveTask(enactmentId, task, patientId, dreSessionId);
                    break;
                default:
                    log.debug("Wrong interactive value");
                    break;
            }
        } else {
            log.debug("Missing interactive node");
        }
    }


    private void handleInteractiveTask(String enactmentId, PlanTask task, String patientId, String dreSessionId) {
        JsonNode metaProperties = task.getMetaprops();
        if (!metaProperties.findValue("resourceType").isNull()) {
            switch (metaProperties.get("resourceType").asText()) {
                case "MedicationRequest":
                    handleInteractiveMedicationRequest(enactmentId, task, patientId, dreSessionId, metaProperties);
                    break;
                default:
                    log.debug("Wrong resourceType value");
            }
        }
    }

    //TODO fill gaps
    private void handleInteractiveMedicationRequest(String enactmentId, PlanTask task, String patientId,
                                                    String dreSessionId, JsonNode metaProperties) {
        if (!metaProperties.findValue("resource").isNull()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                MedicationRequest medicationRequest = objectMapper.
                        readValue(
                                metaProperties.get("resource").asText(), MedicationRequest.class
                        );
                boolean ifTaskAlreadyExists = false;
                ArrayList<Task> tasks = null;//hapiRequestService.getTasks(Task.TaskStatus.REQUESTED);
                for (Task task_ : tasks) {
                    if (task_.getFor().getReference().equals(patientId)) {
                        if (task_.getFocus().getType().equals("MedicationRequest")) {
                            MedicationRequest mR = hapiRequestService.
                                    getMedicationRequestById(task_.getFocus().getReference());
                            if (mR.getCategory().equals(medicationRequest.getCategory())) {
                                log.debug("Task with given code already exist");
                                ifTaskAlreadyExists = true;
                                if (mR.getStatus()
                                        .equals(MedicationRequest.MedicationRequestStatus.ACTIVE)) {
                                    log.debug("Medication request affiliated with task has been activated");
                                    //hapiRequestService.putTask(Task.TaskStatus.COMPLETED);
                                    tryToFinishTask(enactmentId, task, dreSessionId, patientId);
                                }
                                break;
                            }
                        }
                    }
                }
                if (!ifTaskAlreadyExists) {
                    log.debug("Task with given code doesnt exist");
                    String medicationRequestId = hapiRequestService
                            .postMedicationRequest(medicationRequest, MedicationRequest.MedicationRequestStatus.DRAFT,
                                    MedicationRequest.MedicationRequestIntent.PROPOSAL, patientId);
                    prepareRequestedTask(patientId, medicationRequestId);
                    hapiRequestService
                            .createCommunication(Communication.CommunicationStatus.PREPARATION, medicationRequestId);
                    log.debug("Put communication resource with reference at medication request in HAPI FHIR");
                }

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        } else {
            log.debug("Missing resource Node");
        }
    }


    private void handleAutomaticTask(String enactmentId, PlanTask task, String procedure, String patientId, String dreSessionId) {
        deonticsRequestService
                .postEnact(procedure, patientId)
                .subscribe(postEnactResult ->
                        deonticsRequestService
                                .getEnactmentsByEnactmentId(postEnactResult.getEnactmentid())
                                .subscribe(enactments -> {
                                    log.debug("Started new enact for currently processed patient with id: " +
                                            patientId + " new pathway: " + procedure);
                                    handleEnactment(enactments[0], patientId);
                                    tryToFinishTask(enactmentId, task, dreSessionId, patientId);
                                }, getEnactException -> {
                                }), postEnactException -> {
                });
    }

    private void tryToFinishTask(String enactmentId, PlanTask planTask, String dreSessionId, String patientId) {
        deonticsRequestService
                .getQueryConfirmTask(planTask.getName(), dreSessionId)
                .subscribe(queryConfirmTask -> {
                    if (queryConfirmTask == null) {
                        deonticsRequestService
                                .putConfirmTask(planTask.getName(), dreSessionId)
                                .subscribe(confirmTaskOutput -> {
                                    if (!confirmTaskOutput.getState().equals("completed")) {
                                        log.debug("Cannot complete task");
                                    } else {
                                        log.debug("Task has been completed for patient with id: " + patientId);
                                        deonticsRequestService
                                                .getPlanTasksUnderTask(DEONTICS_IN_PROGRESS_STATUS, planTask.getName(), dreSessionId)
                                                .subscribe(tasks -> handleTasks(enactmentId, patientId, dreSessionId, Optional.of(tasks)));
                                    }
                                }, confirmTaskException -> {
                                });
                    }
                }, queryConfirmTaskException -> {
                });
    }

    private Optional<Observation> filterObservationListByDate
            (ArrayList<Observation> observations, DateTimeType date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-ddThh:mm:ss+zz:zz");
        observations.sort(Comparator.comparing(o ->
                LocalDateTime.parse(o.getEffectiveDateTimeType().getValue().toString(), formatter)));
        if (observations.get(0).getEffectiveDateTimeType().before(date)) {
            return Optional.of(observations.get(0));
        } else {
            return Optional.empty();
        }
    }

    private Optional<MedicationRequest> filterMedicationRequestListByDate
            (ArrayList<MedicationRequest> medicationRequests, DateTimeType date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-ddThh:mm:ss+zz:zz");
        medicationRequests.sort(Comparator.comparing(mR -> LocalDateTime.parse(mR.getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod()
                .getEndElement().getValue().toString(), formatter)));

        DateTimeType dosageStartDate = medicationRequests.get(0).getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod().getStartElement();

        DateTimeType dosageEndDate = medicationRequests.get(0).getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod().getEndElement();

        if (dosageStartDate.before(date) && dosageEndDate.after(date)) {
            return Optional.of(medicationRequests.get(0));
        } else {
            return Optional.empty();
        }
    }


}
