package com.capable.physiciandss.utils;

import lombok.Data;
import org.hl7.fhir.r4.model.Coding;

import java.util.HashMap;
import java.util.Map;


@Data
public class OntologyCodingHandlingDeontics {
    private static Map<String, String> systemDictionary;

    static {
        systemDictionary = new HashMap<>();
        systemDictionary.put("SCT", Constants.SNOMED_CODING_HAPI);
    }

    private Coding coding;


    public OntologyCodingHandlingDeontics(String deonticsOntologyCoding) {
        String deonticsSystem = deonticsOntologyCoding.split(":")[0];
        if (OntologyCodingHandlingDeontics.systemDictionary.containsKey(deonticsSystem)) {
            this.coding.setSystem(OntologyCodingHandlingDeontics.systemDictionary.get(deonticsSystem));
        }
        this.coding.setCode(deonticsOntologyCoding.split(":")[1]);
    }

    public String getCode() {
        return coding.getCode();
    }

    public String getSystem() {
        return coding.getSystem();
    }

}
