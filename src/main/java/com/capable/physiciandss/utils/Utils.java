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

/**
 * Pomocnicza klasa zawierająca pomocniczne funkcje
 */
public class Utils {
    /**
     * @param code    Kod pierwszej bytu w zadanym - przez parametr system - systemie
     * @param code2   Kod drugiego bytu w zadanym - przez parametr system2 - systemie
     * @param system  Terminologia systemu pierwszego bytu
     * @param system2 Terminologia systemu drugiego bytu
     * @return Wartość logiczna określająca równość Coding
     */
    public static boolean isCodingMatching(String code, String code2, String system, String system2) {
        if (code == null || code2 == null || system == null || system2 == null)
            return false;
        return code
                .equals(code2)
                && system
                .equals(system2);
    }

    /**
     * @param Date       Data bazowa
     * @param firstDate  Pierwsza porównywana data
     * @param secondDate Druga porównywana data
     * @return Wartość logiczną określającą czy bazowa data znajduje się pomiędzy dwoma pozostałymi
     */
    public static boolean isBetweenDates(DateTimeType Date, DateTimeType firstDate, DateTimeType secondDate) {
        if (firstDate.after(secondDate)) {
            return Date.before(firstDate)
                    && Date.after(secondDate);
        } else {
            return Date.before(secondDate)
                    && Date.after(firstDate);
        }
    }

    /**
     * @param daysCount Liczba dni
     * @return Data sprzed daysCount dni
     */
    public static DateTimeType getDateBeforeCurrentDate(int daysCount) {
        DateTimeType yesterdayDate = new DateTimeType(new Date());
        yesterdayDate.add(5, -daysCount);
        return yesterdayDate;
    }

    /**
     * @param medicationRequests Lista recept
     * @return Jeśli istnieje, zwraca najdłużesz jeszcze aktywną receptę z listy recept
     */
    public static Optional<MedicationRequest> getNewestMedicationRequestFromList
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

    /**
     * @param observations Lista obserwacji
     * @return zwraca najnowszą obserwację, której data wykonania nie przekracza obecnej daty
     */
    public static Optional<Observation> getNewestObservationFromList
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
