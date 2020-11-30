package com.capable.physiciandss.services;

import com.capable.physiciandss.configuration.HapiConnectionConfig;
import com.capable.physiciandss.hapi.Connection;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class HapiRequestService {

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private Connection connection;

    HapiRequestService(){
        ApplicationContext context = new AnnotationConfigApplicationContext(HapiConnectionConfig.class);
        connection = context.getBean("connection", Connection.class);
        log.info("HapiRequestService has been created.");
    }
}
