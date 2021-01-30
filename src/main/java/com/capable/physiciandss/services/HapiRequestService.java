package com.capable.physiciandss.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.capable.physiciandss.configuration.HapiConnectionConfig;
import com.capable.physiciandss.hapi.Connection;
import com.capable.physiciandss.utils.ReferenceHandler;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa zapewniająca komunikacje z platformą danych HAPI FHIR
 */
@Service
public class HapiRequestService {

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final IGenericClient client;
    private final FhirContext ctx;

    /**
     * Nawiązuje połączenie z platformą danych, z którego następnie korzysta reszta metod z klasy.
     * Inicjalizuje loggera
     */
    public HapiRequestService() {
        ApplicationContext context = new AnnotationConfigApplicationContext(HapiConnectionConfig.class);
        client = context.getBean("connection", Connection.class).getClient();
        ctx = context.getBean("connection", Connection.class).getCtx();
        log.info("HapiRequestService has been created.");
    }

    /**
     * @param id Identyfikator pacjenta w platformie danych
     * @return Pacjent o zadanym identyfikatorze
     */
    public Patient getPatient(String id) {
        log.info("Reading patient with id: " + id);
        return client.read()
                .resource(Patient.class)
                .withId(id)
                .execute();
    }

    /**
     * @param id Identyfikator obserwacji w platformie danych
     * @return Obserwacja o zadanym identyfikatorze
     */
    public Observation getObservation(String id) {
        log.info("Reading observation with id: " + id);
        return client.read()
                .resource(Observation.class)
                .withId(id)
                .execute();
    }

    /**
     * @param id Identyfikator komunikacji w platformie danych
     * @return Komunikacja o zadanym identyfikatorze
     */
    public Communication getCommunication(String id) {
        log.info("Reading communication with id: " + id);
        return client.read()
                .resource(Communication.class)
                .withId(id)
                .execute();
    }

    /**
     * @param subjectId Identyfikator pacjenta
     * @return Lista Obserwacji powiązanych z pacjentem o zadanym identyfikatorze
     */
    public List<Observation> getObservationList(String subjectId) {
        log.info("Getting list of observation for subject with id: " + subjectId);
        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(subjectId))
                .returnBundle(Bundle.class)
                .execute();
        return getObservations(bundle);
    }

    /**
     * @param subjectId      Identyfikator pacjenta
     * @param system         Terminologia systemu
     * @param ontologyCoding Medyczny byt w zadanej terminologii systemu
     * @return Lista Obserwacji medycznego bytu, określonego poprzez system oraz ontologyCoding,
     * powiązanych z pacjentem o zadanym identyfikatorze
     */
    public List<Observation> getObservationList(String subjectId, String system, String ontologyCoding) {
        log.info("Getting list of observations with system:" + system + ", ontologyCoding: "
                + ontologyCoding + " for subject with id: " + subjectId);
        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(subjectId))
                .and(Observation.CODE.exactly().systemAndCode(system, ontologyCoding))
                .returnBundle(Bundle.class)
                .execute();
        return getObservations(bundle);
    }

    /**
     * Pomocnicza metoda służąca przenoszeniu obiektów z HL7 Bundle do Java List
     *
     * @param bundle Paczka z obserwacjami
     * @return Lista obserwacji uzyskana z paczki
     */
    private List<Observation> getObservations(Bundle bundle) {
        List<Observation> observations =
                new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, Observation.class));
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            observations.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, Observation.class));
        }
        return observations;
    }

    /**
     * @param id Identyfikator recepty
     * @return Receptę o zadanym identyfikatorze
     */
    public MedicationRequest getMedicationRequest(String id) {
        log.info("Reading medicationRequest with id: " + id);
        return client.read()
                .resource(MedicationRequest.class)
                .withId(id)
                .execute();
    }

    /**
     * @param subjectId Identyfikator pacjenta
     * @return Lista recept powiązanych z pacjentem o zadanym identyfikatorze
     */
    public List<MedicationRequest> getMedicationRequestList(String subjectId) {
        log.info("Getting list of medicationRequests for subject with id: " + subjectId);
        Bundle bundle = client
                .search()
                .forResource(MedicationRequest.class)
                .where(MedicationRequest.SUBJECT.hasId(subjectId))
                .returnBundle(Bundle.class)
                .execute();
        return getMedicationRequests(bundle);
    }

    /**
     * @param subjectId      Identyfikator pacjenta
     * @param system         Terminologia systemu
     * @param ontologyCoding Medyczny byt w zadanej terminologii systemu
     * @param status         Status w jakim ma znajdować się recepta (np. ACTIVE)
     * @return Lista Obserwacji medycznego bytu, określonego poprzez system oraz ontologyCoding,
     * powiązanych z pacjentem o zadanym identyfikatorze
     */
    public List<MedicationRequest> getMedicationRequestList(String subjectId, String system, String ontologyCoding,
                                                            MedicationRequest.MedicationRequestStatus status) {
        log.info("Getting list of medicationRequests with system:" + system + ", ontologyCoding: " + ontologyCoding +
                ", status: " + status.toCode() + " for subject with id: " + subjectId);
        Bundle bundle = client
                .search()
                .forResource(MedicationRequest.class)
                .where(MedicationRequest.SUBJECT.hasId(subjectId))
                .and(MedicationRequest.CODE.exactly().systemAndCode(system, ontologyCoding))
                .and(MedicationRequest.STATUS.exactly().code(status.toCode()))
                .returnBundle(Bundle.class)
                .execute();
        return getMedicationRequests(bundle);
    }

    /**
     * Pomocnicza metoda służąca przenoszeniu obiektów z HL7 Bundle do Java List
     *
     * @param bundle Paczka z receptami
     * @return Lista recept uzyskana z paczki
     */
    private List<MedicationRequest> getMedicationRequests(Bundle bundle) {
        List<MedicationRequest> medicationRequests =
                new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, MedicationRequest.class));
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            medicationRequests.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, MedicationRequest.class));
        }
        return medicationRequests;
    }

    /**
     * @param status Status w jakim ma znajdywać się Komunikacja (np. PREPARATION)
     * @return Lista komunikacji z zadaną wartością pola status
     */
    public List<Communication> getCommunicationList(Communication.CommunicationStatus status) {
        log.info("Getting list of communications with status: " + status.toCode());
        Bundle bundle = client
                .search()
                .forResource(Communication.class)
                .where(Communication.STATUS.exactly().code(status.toCode()))
                .returnBundle(Bundle.class)
                .execute();

        List<Communication> communications =
                new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, Communication.class));
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            communications.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, Communication.class));
        }
        return communications;
    }

    /**
     * @param patientId         Identyfikator pacjenta
     * @param coding Medyczny byt w zadanej terminologii systemu
     * @param status         Status z jakim zostanie utworzona obserwacja
     * @return Referencje na utworzony zasób (np. Observation/1)
     */
    public String createObservation(String patientId, Coding coding, Observation.ObservationStatus status) {
        log.info("Creating observation with system: " + coding.getSystem() + ", ontologyCoding: " +
                coding.getCode() + " with status: " + status.toCode());
        Observation observation = new Observation();
        CodeableConcept codeableConcept = new CodeableConcept();
        ArrayList<Coding> codings = new ArrayList<>();
        codings.add(coding);
        codeableConcept.setCoding(codings);
        observation.setCode(codeableConcept);
        observation.setStatus(status);

        Reference reference = new ReferenceHandler(patientId).getReference();
        observation.setSubject(reference);
        MethodOutcome outcome = client.create().resource(observation).execute();

        log.debug(outcome.toString());

        return outcome.getId().getResourceType() + '/' + outcome.getId().getIdPart();
    }

    /**
     * @param observation Istniejąca obserwacja w platformie danych
     * @param status      Nowa wartość pola status dla zadanej obserwacji
     */
    public void updateObservation(Observation observation, Observation.ObservationStatus status) {
        log.info("Updating communication status");
        observation.setStatus(status);

        MethodOutcome outcome = client.update().resource(observation).execute();

        log.debug(outcome.toString());
    }

    /**
     * @param medicationRequest       Recepta z uzupełnionymi podstawowymi polami tzn. medicationCodeableConcept oraz dosageInstruction
     * @param status                  Określa status tworzonej recepty
     * @param medicationRequestIntent Określa w jakim celu została utworzona recepta
     * @param patientId               Określa dla jakiego pacjenta jest tworzona recepta
     * @return Referencje na utworzony zasób (np. MedicationRequest/1)
     */
    public String createMedicationRequest(MedicationRequest medicationRequest,
                                          MedicationRequest.MedicationRequestStatus status,
                                          MedicationRequest.MedicationRequestIntent medicationRequestIntent,
                                          String patientId) {
        log.info("Posting medicationRequest");
        medicationRequest.setStatus(status);
        medicationRequest.setIntent(medicationRequestIntent);
        Reference reference = new ReferenceHandler(patientId).getReference();
        medicationRequest.setSubject(reference);

        MethodOutcome outcome = client.create().resource(medicationRequest).execute();

        log.debug(outcome.toString());

        return outcome.getId().getResourceType() + '/' + outcome.getId().getIdPart();
    }

    /**
     * @param status      Określa status tworzonej komunikacji
     * @param referenceId Określa wartość referencji znajdującej się w polu payload tworzonej komunikacji
     * @return Referencje na utworzony zasób (np. Communication/1)
     */
    public String createCommunication(Communication.CommunicationStatus status, String referenceId) {
        log.info("Creating communication with status: " + status.toCode() + ", referenceId: " + referenceId);
        Communication communication = new Communication();
        communication.setStatus(status);
        Reference reference = new ReferenceHandler(referenceId).getReference();
        Communication.CommunicationPayloadComponent payloadComponent = new Communication.CommunicationPayloadComponent();
        payloadComponent.setContent(reference);
        ArrayList<Communication.CommunicationPayloadComponent> communicationPayloadComponents = new ArrayList<>();
        communicationPayloadComponents.add(payloadComponent);
        communication.setPayload(communicationPayloadComponents);

        MethodOutcome outcome = client.create().resource(communication).execute();

        log.debug(outcome.toString());

        return outcome.getId().getResourceType() + '/' + outcome.getId().getIdPart();
    }

    /**
     * @param communication Istniejąca komunikacja w platformie danych
     * @param status        Nowa wartość pola status dla zadanej komunikacji
     */
    public void updateCommunication(Communication communication, Communication.CommunicationStatus status) {
        log.info("Updating communication status");
        communication.setStatus(status);

        MethodOutcome outcome = client.update().resource(communication).execute();

        log.debug(outcome.toString());
    }

    /**
     * @param status Określa status zadań, które będą pobrane z platformy danych
     * @return Lista zadań o zadanym statusie
     */
    public List<Task> getTaskList(Task.TaskStatus status) {
        log.info("Getting list of tasks with status: " + status.toCode());
        Bundle bundle = client
                .search()
                .forResource(Task.class)
                .where(Task.STATUS.exactly().code(status.toCode()))
                .returnBundle(Bundle.class)
                .execute();

        List<Task> tasks =
                new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, Task.class));
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client
                    .loadPage()
                    .next(bundle)
                    .execute();
            tasks.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, Task.class));
        }
        return tasks;
    }

    /**
     * @param patient  Referencja na pacjenta, która będzie wpisana w pole "For" zadania
     * @param resource Referencja na zasób powiązany z pacjentem, która będzie wpisana w pole "Focus" zadania
     */
    public void createTask(Reference patient, Reference resource) {
        Task task = new Task();
        task.setIntent(Task.TaskIntent.ORDER);
        task.setStatus(Task.TaskStatus.REQUESTED);
        task.setFor(patient);
        task.setFocus(resource);

        MethodOutcome outcome = client.create().resource(task).execute();

        log.debug(outcome.toString());
    }

    /**
     * @param task   Istniejące zadanie w platformie danych
     * @param status Nowa wartość pola status dla zadanego zadania
     */
    public void updateTask(Task task, Task.TaskStatus status) {
        task.setStatus(status);

        MethodOutcome outcome = client.update().resource(task).execute();

        log.debug(outcome.toString());
    }
}
