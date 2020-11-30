package com.capable.physiciandss.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Connection {

    private IGenericClient client;

    public Connection(String url) {
        FhirContext ctx = FhirContext.forR4();
        this.client = ctx.newRestfulGenericClient(url);
    }
}
