package com.capable.physiciandss.services;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.capable.physiciandss.configuration.HapiConnectionConfig;
import com.capable.physiciandss.hapi.Connection;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class HapiRequestService {

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private IGenericClient client;

    public HapiRequestService(){
        ApplicationContext context = new AnnotationConfigApplicationContext(HapiConnectionConfig.class);
        client = context.getBean("connection", Connection.class).getClient();
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

    public MedicationRequest getMedicationRequestById(String id){
        return client.read()
                .resource(MedicationRequest.class)
                .withId(id)
                .execute();
    }
}
