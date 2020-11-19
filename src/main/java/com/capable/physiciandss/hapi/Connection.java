package com.capable.physiciandss.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class Connection {
    private FhirContext ctx;
    private IGenericClient client;

    public Connection(String url) {
        this.ctx = FhirContext.forR4();
        this.client = ctx.newRestfulGenericClient(url);
    }

    public IGenericClient getClient() {
        return client;
    }

    public FhirContext getCtx() {
        return ctx;
    }
}
