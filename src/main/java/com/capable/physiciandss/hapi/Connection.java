package com.capable.physiciandss.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Getter;
import lombok.Setter;

/**
 * Klasa utrzymująca połączenie z serwerem HAPI FHIR
 */
@Getter
@Setter
public class Connection {

    private IGenericClient client;
    private FhirContext ctx;

    public Connection(String url) {
        this.ctx = FhirContext.forR4();
        this.client = ctx.newRestfulGenericClient(url);
    }
}
