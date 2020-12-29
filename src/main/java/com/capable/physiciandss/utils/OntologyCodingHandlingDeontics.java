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
        systemDictionary.put(Constants.SNOMED_CODING_DEONTICS, Constants.SNOMED_CODING_HAPI);
    }

    private Coding coding = new Coding();


    public OntologyCodingHandlingDeontics(String deonticsOntologyCoding) {
        String[] splittedString = deonticsOntologyCoding.split(":");
        if (OntologyCodingHandlingDeontics.systemDictionary.containsKey(splittedString[0])) {
            this.coding.setSystem(OntologyCodingHandlingDeontics.systemDictionary.get(splittedString[0]));
        }
        this.coding.setCode(splittedString[1].split(" ")[0]);
    }

    public String getCode() {
        return coding.getCode();
    }

    public String getSystem() {
        return coding.getSystem();
    }

}
