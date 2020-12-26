package com.capable.physiciandss.utils;

import lombok.Data;
import org.hl7.fhir.r4.model.Reference;

@Data
public class ReferenceHandling {
    private Reference reference;

    public ReferenceHandling(String reference) {
        this.reference = new Reference(reference);
        this.reference.setType(reference.split("/")[0]);
        this.reference.setId(reference.split("/")[1]);
    }
}
