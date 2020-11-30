package com.capable.physiciandss.services;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.capable.physiciandss.configuration.HapiConnectionConfig;
import com.capable.physiciandss.hapi.Connection;
import org.hl7.fhir.r4.model.Bundle;
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

    public Patient getPatientbyId(String id){
        return client.read()
                .resource(Patient.class)
                .withId(id)
                .execute();
    }
}
