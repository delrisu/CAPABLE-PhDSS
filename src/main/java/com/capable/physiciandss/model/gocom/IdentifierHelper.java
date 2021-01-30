package com.capable.physiciandss.model.gocom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.Identifier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentifierHelper {
    String value;
    public IdentifierHelper(Identifier identifier){
        this.value = identifier.getValue();
    }
}
