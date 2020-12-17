package com.capable.physiciandss.services;

import ca.uhn.fhir.context.FhirContext;
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
    private IGenericClient client;
    private FhirContext ctx;

    public HapiRequestService(){
        ApplicationContext context = new AnnotationConfigApplicationContext(HapiConnectionConfig.class);
        client = context.getBean("connection", Connection.class).getClient();
        ctx = context.getBean("connection", Connection.class).getCtx();
        log.info("HapiRequestService has been created.");
    }

    public Patient getPatientById(String id){
        return client.read()
                .resource(Patient.class)
                .withId(id)
                .execute();
    }

    public Observation getObservationById(String id){
        return client.read()
                .resource(Observation.class)
                .withId(id)
                .execute();
    }

    public List<Observation> getObservationListByPatientId(String id){
        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(id))
                .returnBundle(Bundle.class)
                .execute();
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

    public MedicationRequest getMedicationRequestById(String id){
        return client.read()
                .resource(MedicationRequest.class)
                .withId(id)
                .execute();
    }

    public List<MedicationRequest> getMedicationRequestListByPatientId(String id){
        Bundle bundle = client
                .search()
                .forResource(MedicationRequest.class)
                .where(MedicationRequest.SUBJECT.hasId(id))
                .returnBundle(Bundle.class)
                .execute();
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

    public List<Communication> getCommunicationListByStatus(Communication.CommunicationStatus status){
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
        return  communications;
    }
}
