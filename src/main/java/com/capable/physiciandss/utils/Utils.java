package com.capable.physiciandss.utils;

import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

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

    public static Optional<MedicationRequest> GetNewestMedicationRequestFromList
            (ArrayList<MedicationRequest> medicationRequests) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HAPI_DATETIMETYPE_FORMAT_MR);
        if (medicationRequests.size() == 0)
            return Optional.empty();
        medicationRequests.sort(Comparator.comparing(mR -> LocalDate.parse(mR.getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod()
                .getEndElement().asStringValue(), formatter)));

        DateTimeType dosageStartDate = medicationRequests.get(medicationRequests.size() - 1).getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod().getStartElement();

        DateTimeType dosageEndDate = medicationRequests.get(medicationRequests.size() - 1).getDosageInstruction()
                .get(0).getTiming().getRepeat().getBoundsPeriod().getEndElement();
        if (Utils.isBetweenDates(new DateTimeType(new Date()), dosageStartDate, dosageEndDate)) {
            return Optional.of(medicationRequests.get(0));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Observation> GetNewestObservationFromList
            (ArrayList<Observation> observations) {
        if (observations.size() == 0)
            return Optional.empty();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HAPI_DATETIMETYPE_FORMAT_OBS);
        observations.sort(Comparator.comparing(o ->
                LocalDate.parse(o.getEffectiveDateTimeType().asStringValue(), formatter)));
        if (observations.get(observations.size() - 1).getEffectiveDateTimeType().before(new DateTimeType(new Date()))) {
            return Optional.of(observations.get(0));
        } else {
            return Optional.empty();
        }
    }

}
