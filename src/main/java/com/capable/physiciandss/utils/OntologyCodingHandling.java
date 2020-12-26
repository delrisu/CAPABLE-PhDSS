package com.capable.physiciandss.utils;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;


@Data
public class OntologyCodingHandling {
    private static Map<String, String> systemDictionary;

    static {
        systemDictionary = new HashMap<>();
        systemDictionary.put("SCT", "http://snomed.info/sct");
    }

    private String system;
    private String code;

    public OntologyCodingHandling(String deonticsOntologyCoding) {
        String deonticsSystem = deonticsOntologyCoding.split(":")[0];
        if (OntologyCodingHandling.systemDictionary.containsKey(deonticsSystem)) {
            this.system = OntologyCodingHandling.systemDictionary.get(deonticsSystem);
        }
        this.code = deonticsOntologyCoding.split(":")[1];
    }
}
