package com.capable.physiciandss.model.gocom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.Reference;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceHelper {
    String reference;
    String type;
    IdentifierHelper identifier;

    public ReferenceHelper(Reference reference){
        this.identifier = new IdentifierHelper(reference.getIdentifier());
        this.type = reference.getType();
        this.reference = reference.getReference();
    }
}