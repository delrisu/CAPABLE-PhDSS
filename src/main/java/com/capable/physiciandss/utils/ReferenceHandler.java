package com.capable.physiciandss.utils;

import lombok.Data;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;

@Data
public class ReferenceHandler {
    private Reference reference;

    public ReferenceHandler(String reference) {
        this.reference = new Reference(reference);
        this.reference.setType(reference.split("/")[0]);
        Identifier identifier = new Identifier();
        identifier.setValue(reference.split("/")[1]);
        this.reference.setIdentifier(identifier);
    }
}
