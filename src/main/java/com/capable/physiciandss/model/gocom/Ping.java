package com.capable.physiciandss.model.gocom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.Reference;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ping {
    Reference medicationRequestReference;
}
