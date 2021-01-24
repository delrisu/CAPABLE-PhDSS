package com.capable.physiciandss.utils;

import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.capable.physiciandss.utils.Constants.HAPI_DATETIMETYPE_FORMAT_MR;
import static com.capable.physiciandss.utils.Constants.HAPI_DATETIMETYPE_FORMAT_OBS;

public class Utils {
    public static boolean isCodingMatching(String code, String code2, String system, String system2) {
        if (code == null || code2 == null || system == null || system2 == null)
            return false;
        return code
                .equals(code2)
                && system
                .equals(system2);
    }

    public static boolean isBetweenDates(DateTimeType Date, DateTimeType firstDate, DateTimeType secondDate) {
        if (firstDate.after(secondDate)) {
            return Date.before(firstDate)
                    && Date.after(secondDate);
        } else {
            return Date.before(secondDate)
                    && Date.after(firstDate);
        }
    }

    public static DateTimeType getDateBeforeCurrentDate(int daysCount) {
        DateTimeType yesterdayDate = new DateTimeType(new Date());
        yesterdayDate.add(5, -daysCount);
        return yesterdayDate;
    }

    public static Optional<MedicationRequest> getNewestMedicationRequestFromList
            (ArrayList<MedicationRequest> medicationRequests) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HAPI_DATETIMETYPE_FORMAT_MR);
        if (medicationRequests != null && medicationRequests.size() != 0) {
            medicationRequests.removeIf(medicationRequest -> medicationRequest.getDosageInstruction() == null
                    || medicationRequest.getDosageInstruction().get(0) == null
                    || medicationRequest.getDosageInstruction().get(0).getTiming() == null
                    || medicationRequest.getDosageInstruction().get(0).getTiming().getRepeat() == null
                    || medicationRequest.getDosageInstruction().get(0).getTiming().getRepeat().getBoundsPeriod() == null
                    || medicationRequest.getDosageInstruction().get(0).getTiming().getRepeat().getBoundsPeriod() == null
                    || medicationRequest.getDosageInstruction().get(0).getTiming().getRepeat().getBoundsPeriod().getStartElement() == null
                    || medicationRequest.getDosageInstruction().get(0).getTiming().getRepeat().getBoundsPeriod().getEndElement() == null);
            if (medicationRequests.size() > 0) {
                medicationRequests.sort(Comparator.comparing(mR -> LocalDate.parse(mR.getDosageInstruction()
                        .get(0).getTiming().getRepeat().getBoundsPeriod()
                        .getEndElement().asStringValue(), formatter)));
                Collections.sort(medicationRequests, Collections.reverseOrder());
                for (MedicationRequest medicationRequest : medicationRequests) {
                    DateTimeType dosageStartDate = medicationRequest.getDosageInstruction()
                            .get(0).getTiming().getRepeat().getBoundsPeriod().getStartElement();
                    DateTimeType dosageEndDate = medicationRequest.getDosageInstruction()
                            .get(0).getTiming().getRepeat().getBoundsPeriod().getEndElement();
                    if (Utils.isBetweenDates(new DateTimeType(new Date()), dosageStartDate, dosageEndDate)) {
                        return Optional.of(medicationRequest);
                    }
                }
            }
        }
        return Optional.empty();
    }


    public static Optional<Observation> getNewestObservationFromList
            (ArrayList<Observation> observations) {
        if (observations != null && observations.size() != 0) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HAPI_DATETIMETYPE_FORMAT_OBS);
            observations.removeIf(observation -> observation.getEffectiveDateTimeType() == null ||
                    observation.getEffectiveDateTimeType().getValue() == null);
            if (observations.size() > 0) {
                observations.sort(Comparator.comparing(o ->
                        LocalDate.parse(o.getEffectiveDateTimeType().asStringValue(), formatter)));
                Collections.sort(observations, Collections.reverseOrder());
                for (Observation observation : observations) {
                    if (observation.getEffectiveDateTimeType().before(new DateTimeType(new Date()))) {
                        return Optional.of(observation);
                    }
                }
            }
        }
        return Optional.empty();
    }
}

