package com.capable.physiciandss.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.capable.physiciandss.configuration.HapiConnectionConfig;
import com.capable.physiciandss.hapi.Connection;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HapiRequestService {

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final IGenericClient client;
    private final FhirContext ctx;

    public HapiRequestService() {
        ApplicationContext context = new AnnotationConfigApplicationContext(HapiConnectionConfig.class);
        client = context.getBean("connection", Connection.class).getClient();
        ctx = context.getBean("connection", Connection.class).getCtx();
        log.info("HapiRequestService has been created.");
    }

    public Patient getPatient(String id) {
        log.info("Reading patient with id: " + id);
        return client.read()
                .resource(Patient.class)
                .withId(id)
                .execute();
    }

    public Observation getObservation(String id) {
        log.info("Reading observation with id: " + id);
        return client.read()
                .resource(Observation.class)
                .withId(id)
                .execute();
    }

    public Communication getCommunication(String id) {
        log.info("Reading communication with id: " + id);
        return client.read()
                .resource(Communication.class)
                .withId(id)
                .execute();
    }

    public List<Observation> getObservationList(String subjectId) {
        log.info("Getting list of observation for subject with id: " + subjectId);
        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(subjectId))
                .returnBundle(Bundle.class)
                .execute();
        return getObservations(bundle);
    }

    public List<Observation> getObservationList(String subjectId, String system, String ontologyCoding) {
        log.info("Getting list of observations with system:" + system + ", ontologyCoding: "
                + ontologyCoding + " for subject with id: " + subjectId);
        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(subjectId))
                .and(Observation.CODE.exactly().systemAndCode(system, ontologyCoding))
                .returnBundle(Bundle.class)
                .execute();
        return getObservations(bundle);
    }

    private List<Observation> getObservations(Bundle bundle) {
        List<Observation> observations =
                new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, Observation.class));
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            observations.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, Observation.class));
        }
        return observations;
    }

    public MedicationRequest getMedicationRequest(String id) {
        log.info("Reading medicationRequest with id: " + id);
        return client.read()
                .resource(MedicationRequest.class)
                .withId(id)
                .execute();
    }

    public List<MedicationRequest> getMedicationRequestList(String subjectId) {
        log.info("Getting list of medicationRequests for subject with id: " + subjectId);
        Bundle bundle = client
                .search()
                .forResource(MedicationRequest.class)
                .where(MedicationRequest.SUBJECT.hasId(subjectId))
                .returnBundle(Bundle.class)
                .execute();
        return getMedicationRequests(bundle);
    }

    public List<MedicationRequest> getMedicationRequestList(String subjectId, String system, String ontologyCoding,
                                                            MedicationRequest.MedicationRequestStatus status) {
        log.info("Getting list of medicationRequests with system:" + system + ", ontologyCoding: " + ontologyCoding +
                ", status: " + status.toCode() + " for subject with id: " + subjectId);
        Bundle bundle = client
                .search()
                .forResource(MedicationRequest.class)
                .where(MedicationRequest.SUBJECT.hasId(subjectId))
                .and(MedicationRequest.CODE.exactly().systemAndCode(system, ontologyCoding))
                .and(MedicationRequest.STATUS.exactly().code(status.toCode()))
                .returnBundle(Bundle.class)
                .execute();
        return getMedicationRequests(bundle);
    }

    private List<MedicationRequest> getMedicationRequests(Bundle bundle) {
        List<MedicationRequest> medicationRequests =
                new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, MedicationRequest.class));
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            medicationRequests.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, MedicationRequest.class));
        }
        return medicationRequests;
    }

    public List<Communication> getCommunicationList(Communication.CommunicationStatus status) {
        log.info("Getting list of communications with status: " + status.toCode());
        Bundle bundle = client
                .search()
                .forResource(Communication.class)
                .where(Communication.STATUS.exactly().code(status.toCode()))
                .returnBundle(Bundle.class)
                .execute();

        List<Communication> communications =
                new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, Communication.class));
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            communications.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, Communication.class));
        }
        return communications;
    }

    public String createObservation(String system, String ontologyCoding, Observation.ObservationStatus status) {
        log.info("Creating observation with system: " + system + ", ontologyCoding: " +
                ontologyCoding + " with status: " + status.toCode());
        Observation observation = new Observation();
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem(system);
        coding.setCode(ontologyCoding);
        ArrayList<Coding> codings = new ArrayList<>();
        codings.add(coding);
        codeableConcept.setCoding(codings);
        observation.setCode(codeableConcept);
        observation.setStatus(status);

        MethodOutcome outcome = client.create().resource(observation).execute();

        log.debug(outcome.toString());

        return outcome.getId().getValue();
    }

    public void updateObservation(Observation observation, Observation.ObservationStatus status) {
        log.info("Updating communication status");
        observation.setStatus(status);

        MethodOutcome outcome = client.update().resource(observation).execute();

        log.debug(outcome.toString());
    }

    public String createMedicationRequest(MedicationRequest medicationRequest,
                                          MedicationRequest.MedicationRequestStatus status,
                                          MedicationRequest.MedicationRequestIntent medicationRequestIntent,
                                          String patientId) {
        log.info("Posting medicationRequest");
        medicationRequest.setStatus(status);
        medicationRequest.setIntent(medicationRequestIntent);
        Reference reference = new Reference(patientId);
        reference.setIdentifier(new Identifier().setValue(reference.getReference().split("/")[1]));
        reference.setType(reference.getReference().split("/")[0]);
        medicationRequest.setSubject(reference);

        MethodOutcome outcome = client.create().resource(medicationRequest).execute();

        log.debug(outcome.toString());

        return outcome.getId().getValue();
    }

    public String createCommunication(Communication.CommunicationStatus status, String referenceId) {
        log.info("Creating communication with status: " + status.toCode() + ", referenceId: " + referenceId);
        Communication communication = new Communication();
        communication.setStatus(status);
        Reference reference = new Reference(referenceId);
        reference.setIdentifier(new Identifier().setValue(reference.getReference().split("/")[1]));
        reference.setType(reference.getReference().split("/")[0]);
        Communication.CommunicationPayloadComponent payloadComponent = new Communication.CommunicationPayloadComponent();
        payloadComponent.setContent(reference);
        ArrayList<Communication.CommunicationPayloadComponent> communicationPayloadComponents = new ArrayList<>();
        communicationPayloadComponents.add(payloadComponent);
        communication.setPayload(communicationPayloadComponents);

        MethodOutcome outcome = client.create().resource(communication).execute();

        log.debug(outcome.toString());

        return outcome.getId().getValue();
    }

    public void updateCommunication(Communication communication, Communication.CommunicationStatus status) {
        log.info("Updating communication status");
        communication.setStatus(status);

        MethodOutcome outcome = client.update().resource(communication).execute();

        log.debug(outcome.toString());
    }

    public List<Task> getTaskList(Task.TaskStatus status) {
        log.info("Getting list of tasks with status: " + status.toCode());
        Bundle bundle = client
                .search()
                .forResource(Task.class)
                .where(Task.STATUS.exactly().code(status.toCode()))
                .returnBundle(Bundle.class)
                .execute();

        List<Task> tasks =
                new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, Task.class));
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            tasks.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, Task.class));
        }
        return tasks;
    }

    public void createTask(Reference patient, Reference resource) {
        Task task = new Task();
        task.setIntent(Task.TaskIntent.ORDER);
        task.setStatus(Task.TaskStatus.REQUESTED);
        task.setFor(patient);
        task.setFocus(resource);

        MethodOutcome outcome = client.create().resource(task).execute();

        log.debug(outcome.toString());
    }

    public void updateTask(Task task, Task.TaskStatus status) {
        task.setStatus(status);

        MethodOutcome outcome = client.update().resource(task).execute();

        log.debug(outcome.toString());
    }
}
