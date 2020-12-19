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
        return client.read()
                .resource(Patient.class)
                .withId(id)
                .execute();
    }

    public Observation getObservation(String id) {
        return client.read()
                .resource(Observation.class)
                .withId(id)
                .execute();
    }

    public Communication getCommunication(String id) {
        return client.read()
                .resource(Communication.class)
                .withId(id)
                .execute();
    }

    public List<Observation> getObservationList(String id) {
        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(id))
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

    public List<Observation> getObservationList(String id, String system, String ontologyCoding) {
        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(id))
                .and(Observation.CODE.exactly().systemAndCode(system, ontologyCoding))
                .returnBundle(Bundle.class)
                .execute();
        return getObservations(bundle);
    }

    public MedicationRequest getMedicationRequest(String id) {
        return client.read()
                .resource(MedicationRequest.class)
                .withId(id)
                .execute();
    }

    public List<MedicationRequest> getMedicationRequestList(String id) {
        Bundle bundle = client
                .search()
                .forResource(MedicationRequest.class)
                .where(MedicationRequest.SUBJECT.hasId(id))
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

    public List<MedicationRequest> getMedicationRequestList(String id, String system, String ontologyCoding, MedicationRequest.MedicationRequestStatus status) {
        Bundle bundle = client
                .search()
                .forResource(MedicationRequest.class)
                .where(MedicationRequest.SUBJECT.hasId(id))
                .and(MedicationRequest.CODE.exactly().systemAndCode(system, ontologyCoding))
                .and(MedicationRequest.STATUS.exactly().code(status.toCode()))
                .returnBundle(Bundle.class)
                .execute();
        return getMedicationRequests(bundle);
    }

    public List<Communication> getCommunicationList(Communication.CommunicationStatus status) {
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

    public String postMedicationRequest(MedicationRequest medicationRequest,
                                        MedicationRequest.MedicationRequestStatus status,
                                        MedicationRequest.MedicationRequestIntent medicationRequestIntent,
                                        String patientId) {
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
        communication.setStatus(status);

        MethodOutcome outcome = client.update().resource(communication).execute();

        log.debug(outcome.toString());
    }
}
