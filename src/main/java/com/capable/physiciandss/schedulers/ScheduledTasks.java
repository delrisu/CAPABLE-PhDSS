package com.capable.physiciandss.schedulers;

import com.capable.physiciandss.model.deontics.get.Enactment;
import com.capable.physiciandss.model.deontics.get.ItemData;
import com.capable.physiciandss.model.deontics.get.PlanTask;
import com.capable.physiciandss.services.DeonticsRequestService;
import com.capable.physiciandss.services.HapiRequestService;
import com.capable.physiciandss.utils.Constants;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

import static com.capable.physiciandss.utils.Constants.*;

@Component   //TODO popatrzec na ontology coding jaka forme zwraca deontics
public class ScheduledTasks {

    private static final Logger log =
            LoggerFactory.getLogger(ScheduledTasks.class);
    private final HapiRequestService hapiRequestService = new HapiRequestService();
    private final DeonticsRequestService deonticsRequestService = new DeonticsRequestService();


    @Scheduled(fixedRate = 5000)
    @Async
    //TODO dopisać że idziemy po wszystkich communication i robimy listę [patientId : arrayPayloadResourceReference]
    public void checkForDataToProcess() {
        log.info(Constants.SCHEDULER_TASK_INFO);
        ArrayList<Communication> communicationList = (ArrayList<Communication>) hapiRequestService
                .getCommunicationList(Communication.CommunicationStatus.INPROGRESS);
        if (!communicationList.isEmpty()) {
            communicationList.forEach(
                    communication -> {
                        hapiRequestService.updateCommunication(communication, Communication.CommunicationStatus.UNKNOWN);
                    });

            communicationList.forEach(communication ->
            {
                if (!communication.getPayload().isEmpty()) {
                    String payloadType = communication.getPayload().get(0).getContentReference().getType();
                    //TODO Dopisać rozważanie czy payloaadResourceId odnosi sie do reported/intearactive taska
                    Reference payloadResourceReference = communication.getPayload().get(0).getContentReference();
                    Optional<String> patientId;
                    switch (payloadType) {
                        case "MedicationRequest":
                            patientId = Optional.ofNullable(hapiRequestService.
                                    getMedicationRequestById(payloadResourceReference.getReference()).
                                    getSubject().
                                    getReference());
                            log.debug("Found change in Medication Request for patient with id: " + patientId);
                            patientId.ifPresent(pId -> handlePatient(pId, payloadResourceReference));
                            break;
                        case "Patient":
                            patientId = Optional.of(payloadResourceReference.getReference());
                            log.debug("Found information about new Patient with id: " + patientId);
                            patientId.ifPresent(this::handleNewPatient);
                            break;
                        default:
                            log.debug(SCHEDULER_TASK_BAD_PAYLOAD_TYPE);
                    }
                } else {
                    log.debug("Payload is missing");
                }
            });
        }
    }

    private void handleNewPatient(String patientId) {
        deonticsRequestService.getPathwayByName(META_GUIDELINE_NAME).subscribe(pathways -> {
            if (pathways.length < 1) {
                log.info(META_GUIDELINE_MISSING_INFO);
            } else {
                deonticsRequestService
                        .postEnact(pathways[0].getId(), patientId)
                        .subscribe(postEnactResult -> deonticsRequestService
                                .getEnactmentsByEnactmentId(postEnactResult.getEnactmentid()).subscribe(
                                        enactments -> handleEnactment(enactments[0], patientId, Optional.empty()), getEnactException -> {
                                        }), postEnactException -> {
                        });
            }
        }, pathwayException -> {
        });
    }

    private void handlePatient(String patientId, Reference payloadResourceReference) {
        deonticsRequestService.getEnactmentsByPatientId(patientId)
                .subscribe(enactments -> {
                    for (Enactment enactment : enactments
                    ) {
                        handleEnactment(enactment, patientId, Optional.of(payloadResourceReference));
                    }
                }, enactmentException -> {
                });
    }

    private void handleEnactment(Enactment enactment, String patientId, Optional<Reference> payloadResourceReference) {
        deonticsRequestService.getConnect(enactment.getId()).subscribe(
                connect -> handleTasks(enactment.getId(), patientId, connect.getDresessionid(), Optional.empty(), payloadResourceReference), connectionException -> {
                });
    }

    private void handleTasks(String enactmentId, String patientId, String dresessionId, Optional<PlanTask[]> tasksToProcess, Optional<Reference> payloadResourceReference) {
        deonticsRequestService
                .getPlanTasks(DEONTICS_IN_PROGRESS_STATUS, dresessionId)
                .subscribe(tasks -> {
                    if (tasks.length == 0) {
                        deonticsRequestService.putEnactmentDelete(enactmentId, dresessionId).subscribe(enactmentDeleteOutput -> {
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
                                handleTask(enactmentId, patientId, dresessionId, payloadResourceReference, task);
                            }
                        } else {
                            for (PlanTask task : tasks) {
                                handleTask(enactmentId, patientId, dresessionId, payloadResourceReference, task);

                            }
                        }
                    }
                }, planTasksException -> {
                });
    }

    private void handleTask(String enactmentId, String patientId, String dresessionId, Optional<Reference> payloadResourceReference, PlanTask task) {
        switch (task.getType()) {
            case DEONTICS_ENQUIRY_TASK_TYPE:
                log.debug("Found " + DEONTICS_ENQUIRY_TASK_TYPE + " task to process");
                handleEnquiryTask(enactmentId, task, dresessionId, patientId, payloadResourceReference);
                break;
            case DEONTICS_ACTION_TASK_TYPE:
                log.debug("Found " + DEONTICS_ACTION_TASK_TYPE + " task to process");
                handleActionTask(enactmentId, task, dresessionId, patientId, payloadResourceReference);
                break;
            default:
                log.info(SCHEDULER_TASK_BAD_DEONTIC_TASKS_TYPE);
                break;
        }
    }

    private void handleEnquiryTask(String enactmentId, PlanTask task, String dresessionId, String patientId, Optional<Reference> payloadResourceReference) {
        deonticsRequestService.getData(task.getName(), dresessionId).subscribe(itemDataList -> {
            for (ItemData itemData : itemDataList) {
                JsonNode metaproperties = itemData.getMetaprops();
                if (!metaproperties.findValue("source").isNull()) {
                    switch (metaproperties.get("source").asText()) {
                        case "stored":
                            log.debug("Found stored data item to process");
                            handleStoredData(enactmentId, task, itemData, dresessionId, patientId);
                            break;
                        case "abstracted":
                            log.debug("Found abstracted data item to process");
                            handleAbstractedData(enactmentId, task, itemData, dresessionId, patientId);
                            break;
                        case "reported":
                            log.debug("Found reported data item to process");
                            handleReportedData(enactmentId, task, itemData, dresessionId, patientId, payloadResourceReference);
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

    //TODO poprawic ontologyCoding handling
    private void handleReportedData(String enactmentId, PlanTask task, ItemData itemData, String dresessionId, String patientId, Optional<Reference> payloadResourceReference) {
        JsonNode metaproperties = itemData.getMetaprops();
        if (!metaproperties.findValue("resourceType").isNull()) {
            if (!metaproperties.findValue("ontology.coding").isNull()) {
                String system = metaproperties.get("ontology_coding_system").asText();
                String code = metaproperties.get("ontology_coding_code").asText();
                Observation observation = new Observation();
                Coding coding = new Coding().setCode(code).setSystem(system);
                ArrayList<Coding> codings = new ArrayList<>();
                codings.add(coding);
                observation.setCode(new CodeableConcept().setCoding(codings));
                //TODO dodanie obslugi zasoboww task aby kontrolowac podstan tego stanu

            } else {
                log.debug("Missing ontology.coding in metaproperties");
            }
        } else {
            log.debug("Missing resourceType in metaproperties");
        }
        tryToFinishTask(enactmentId, task, dresessionId, patientId);
    }


    private void handleAbstractedData(String enactmentId, PlanTask task, ItemData itemData, String dresessionId, String patientId) {
        //TODO MockKDOM


        tryToFinishTask(enactmentId, task, dresessionId, patientId);
    }

    //TODO poprawic ontologyCoding handling
    private void handleStoredData(String enactmentId, PlanTask task, ItemData itemData, String dresessionId, String patientId) {
        JsonNode metaproperties = itemData.getMetaprops();
        if (!metaproperties.findValue("resourceType").isNull()) {
            if (!metaproperties.findValue("ontology.coding").isNull()) {
                String system = metaproperties.get("ontology_coding_system").asText();
                String code = metaproperties.get("ontology_coding_code").asText();
                switch (metaproperties.get("resourceType").asText()) {
                    case "Observation":
                        log.debug("Checking observation for essential data - patient id: " + patientId);
                        handleStoredObservationData(enactmentId, task, itemData, patientId, dresessionId, system, code);
                        break;
                    case "MedicationRequest":
                        log.debug("Checking medication request for essential data  - patient id: " + patientId);
                        handleStoredMedicationRequestData(enactmentId, task, itemData, patientId, dresessionId, system, code);
                        break;
                }
            } else {
                log.debug("Missing ontology.coding in metaproperties");
            }
        } else {
            log.debug("Missing resourceType in metaproperties");
        }
    }


    private void handleStoredMedicationRequestData(String enactmentId, PlanTask task, ItemData itemData, String patientId, String dresessionId, String system, String code) {
        ArrayList<MedicationRequest> medicationRequests = (ArrayList<MedicationRequest>) hapiRequestService.getMedicationRequestList(patientId, system, code, MedicationRequest.MedicationRequestStatus.ACTIVE);
        Optional<MedicationRequest> medicationRequest = filterMedicationRequestListByDate(medicationRequests, new DateTimeType(new Date()));
        String value = "0";
        if (medicationRequest.isPresent()) {
            value = "1";
        }
        String finalValue = value;
        deonticsRequestService.putDataValue(itemData.getName(), value, dresessionId).subscribe(dataValueOutput -> {
            if (dataValueOutput.isSuccess()) {
                log.debug("Put medication request data value in deontics  for patient with id: " + patientId + " dataValue: " + finalValue);
                tryToFinishTask(enactmentId, task, dresessionId, patientId);
            } else {
                log.debug("Cannot put dataValue");
            }
        });
    }


    private void handleStoredObservationData(String enactmentId, PlanTask task, ItemData itemData, String patientId, String dresessionId, String system, String code) {
        ArrayList<Observation> observations = (ArrayList<Observation>) hapiRequestService.getObservationList(patientId, system, code);
        Optional<Observation> observation = filterObservationListByDate(observations, new DateTimeType(new Date()));
        String valueQuantity = "";
        if (observation.isPresent()) {
            valueQuantity = observation.get().getValueQuantity().toString();
        }
        String finalValueQuantity = valueQuantity;
        deonticsRequestService.putDataValue(itemData.getName(), valueQuantity, dresessionId).subscribe(dataValueOutput -> {
            if (dataValueOutput.isSuccess()) {
                log.debug("Put observation data value in deontics  for patient with id: " + patientId + " dataValue: " + finalValueQuantity);
                tryToFinishTask(enactmentId, task, dresessionId, patientId);
            } else {
                log.debug("Cannot put dataValue");
            }
        });
    }

    private void handleActionTask(String enactmentId, PlanTask task, String dresessionId, String patientId, Optional<Reference> payloadResourceReference) {
        JsonNode metaproperties = task.getMetaprops();
        if (!metaproperties.findValue("interactive").isNull()) {
            switch (metaproperties.get("interactive").asText()) {
                case "0":
                    log.debug("Found automatic task to process for patient with id: " + patientId);
                    handleAutomaticTask(enactmentId, task, task.getProcedure(), patientId, dresessionId);
                    break;
                case "1":
                    log.debug("Found interactive task to process for patient with id: " + patientId);
                    handleInteractiveTask(enactmentId, task, patientId, dresessionId, payloadResourceReference);
                    break;
                default:
                    log.debug("Wrong interactive value");
                    break;
            }
        } else {
            log.debug("Missing interactive node");
        }
    }


    //TODO dopisac sprawdzanie czy reference z payloadu wskazuje na potrzbny zasób
    private void handleInteractiveTask(String enactmentId, PlanTask task, String patientId, String dresessionId, Optional<Reference> payloadResourceReference) {
        JsonNode metaproperties = task.getMetaprops();
        if (!metaproperties.findValue("resourceType").isNull()) {
            switch (metaproperties.get("resourceType").asText()) {
                case "MedicationRequest":
                    if (!metaproperties.findValue("resource").isNull()) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            MedicationRequest medicationRequest = objectMapper.
                                    readValue(
                                            metaproperties.get("resource").asText(), MedicationRequest.class
                                    );
                            String medicationRequestId = hapiRequestService.postMedicationRequest(medicationRequest, MedicationRequest.MedicationRequestStatus.DRAFT, MedicationRequest.MedicationRequestIntent.PROPOSAL, patientId);
                            hapiRequestService.createCommunication(Communication.CommunicationStatus.PREPARATION, medicationRequestId);
                            log.debug("Put communication resource with reference at medication request in HAPI FHIR");
                            //TODO dodanie obslugi zasoboww task aby kontrolowac podstan tego stanu
                            tryToFinishTask(enactmentId, task, dresessionId, patientId);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    } else {
                        log.debug("Missing resource Node");
                    }
                    break;
                default:
                    log.debug("Wrong resourceType value");
            }
        }
    }


    private void handleAutomaticTask(String enactmentId, PlanTask task, String procedure, String patientId, String dresessionId) {
        deonticsRequestService.postEnact(procedure, patientId).subscribe(postEnactResult ->
                deonticsRequestService.getEnactmentsByEnactmentId(postEnactResult.getEnactmentid()).subscribe(
                        enactments -> {
                            log.debug("Started new enact for currently processed patient with id: " + patientId + " new pathway: " + procedure);
                            handleEnactment(enactments[0], patientId, Optional.empty());
                            tryToFinishTask(enactmentId, task, dresessionId, patientId);
                        }, getEnactException -> {
                        }), postEnactException -> {
        });
    }

    private void tryToFinishTask(String enactmentId, PlanTask planTask, String dresessionId, String patientId) {
        deonticsRequestService.getQueryConfirmTask(planTask.getName(), dresessionId).subscribe(queryConfirmTask -> {
            if (queryConfirmTask == null) {
                deonticsRequestService.putConfirmTask(planTask.getName(), dresessionId).subscribe(confirmTaskOutput -> {
                    if (!confirmTaskOutput.getState().equals("completed")) {
                        log.debug("Cannot complete task");
                    } else {
                        log.debug("Task has been completed for patient with id: " + patientId);
                        deonticsRequestService.getPlanTasksUnderTask(DEONTICS_IN_PROGRESS_STATUS, planTask.getName(), dresessionId).subscribe(tasks -> {
                            handleTasks(enactmentId, patientId, dresessionId, Optional.of(tasks), Optional.empty());
                        });
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
