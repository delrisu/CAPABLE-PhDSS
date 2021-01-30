package com.capable.physiciandss.utils;

import lombok.Data;
import org.hl7.fhir.r4.model.Coding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Klasa pomocniczna stworzona, aby ułatwić zmianę formatu systemu oraz coding (pole ontology.codings w metaproperties) z wersji Deonticsa na wersję HL7 FHIR.
 */
@Data
public class OntologyCodingHandlingDeontics {
    private static Map<String, String> systemDictionary;

    static {
        systemDictionary = new HashMap<>();
        systemDictionary.put(Constants.SNOMED_CODING_DEONTICS, Constants.SNOMED_CODING_HAPI);
    }

    private Coding coding = new Coding();


    /**
     * @param deonticsOntologyCoding Ontology coding zapisany w formacie zgodnym z formatem Deonticsa
     */
    public OntologyCodingHandlingDeontics(String deonticsOntologyCoding) {
        String[] splittedString = deonticsOntologyCoding.split(":");
        if (splittedString.length < 2) {

        } else {
            if (OntologyCodingHandlingDeontics.systemDictionary.containsKey(splittedString[0])) {
                this.coding.setSystem(OntologyCodingHandlingDeontics.systemDictionary.get(splittedString[0]));
            }
            splittedString = splittedString[1].split(" ");
            this.coding.setCode(splittedString[0]);
            if (splittedString.length > 1)
                this.coding.setDisplay(String.join(" ",Arrays.copyOfRange(splittedString, 1,splittedString.length)));
        }
    }

    /**
     * @return Kod w formacie zgodnym z formatem HL7 FHIR
     */
    public String getCode() {
        return coding.getCode();
    }

    /**
     * @return System w formacie zgodnym z formatem HL7 FHIR
     */
    public String getSystem() {
        return coding.getSystem();
    }

}
