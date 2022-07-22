package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

public class PatientBuilder {
  
  private final bio.ferlab.clin.portal.forms.models.submit.Patient patient;
  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;

  private Optional<Patient> patientByRamq = Optional.empty();
  private Optional<Patient> patientByMrn = Optional.empty();
  
  private Optional<Person> personByRamq = Optional.empty();
  private Optional<Person> personByMrn = Optional.empty();
  
  public PatientBuilder(FhirClient fhirClient,
                        SubmitToFhirMapper mapper,
                        bio.ferlab.clin.portal.forms.models.submit.Patient patient) {
    this.patient = patient;
    this.fhirClient = fhirClient;
    this.mapper = mapper;
  }
  
  public PatientBuilder validateRamqAndMrn() {
    if (StringUtils.isAllBlank(patient.getRamq(), patient.getMrn())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.ramq and patient.mrn can't be both null");
    }
    return this;
  }
  
  public PatientBuilder validateEp() {
    try {
      this.fhirClient.getGenericClient().read().resource(Organization.class).withId(patient.getEp()).encodedJson().execute();
    }catch(ResourceNotFoundException e){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.ep " + patient.getEp() + " is unknown");
    }
    return this;
  }
  
  public PatientBuilder findByRamq() {
    if(StringUtils.isNotBlank(patient.getRamq())) {
      Bundle bundle = this.fhirClient.getGenericClient().search()
          .forResource(Person.class)
          .where(Person.IDENTIFIER.exactly().code(patient.getRamq()))
          .include(Person.INCLUDE_PATIENT)
          .returnBundle(Bundle.class)
          .encodedJson()
          .execute();

      BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), bundle);
      final Person person = bundleExtractor.getNextResourcesOfType(Person.class);
      final List<Patient> patients = bundleExtractor.getAllResourcesOfType(Patient.class);
      final Optional<Patient> patient = patients.stream().filter(p -> p.getManagingOrganization()
          .getReference().equals(FhirUtils.formatResource(new Organization().setId(this.patient.getEp())))).findFirst();

      this.personByRamq = Optional.ofNullable(person);
      this.patientByRamq = patient;
    }
    return this;
  }
  
  public PatientBuilder findByMrn() {
    if(StringUtils.isNotBlank(patient.getMrn())) {
      Bundle bundle = this.fhirClient.getGenericClient().search()
          .forResource(Patient.class)
          .where(Patient.IDENTIFIER.exactly().code(patient.getMrn()))
          .and(Patient.ORGANIZATION.hasId(patient.getEp()))
          .revInclude(Person.INCLUDE_PATIENT)
          .returnBundle(Bundle.class)
          .encodedJson()
          .execute();

      BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), bundle);
      final Patient patient = bundleExtractor.getNextResourcesOfType(Patient.class);
      final Person person = bundleExtractor.getNextResourcesOfType(Person.class);

      this.patientByMrn = Optional.ofNullable(patient);
      this.personByMrn = Optional.ofNullable(person);
    }
    return this;
  }
  
  public Result build(){
    
    Optional<Person> existingPerson = this.personByRamq.or(() -> this.personByMrn);
    Optional<Patient> existingPatient = this.patientByRamq.or(() -> this.patientByMrn);
    
    // update existing patient
    existingPatient.ifPresent(p -> mapper.updatePatient(patient, p));
    // keep existing or create new patient
    final Patient newOrUpdatedPatient = existingPatient.orElse(mapper.mapToPatient(patient));
    // update existing person
    existingPerson.ifPresent(p -> mapper.updatePerson(patient, p, newOrUpdatedPatient));
    // keep existing or create new person
    final Person newOrUpdatedPerson = existingPerson.orElse(mapper.mapToPerson(patient, newOrUpdatedPatient));
    
    return new Result(newOrUpdatedPatient, existingPatient.isEmpty(), newOrUpdatedPerson, existingPerson.isEmpty());
  }
    
  @AllArgsConstructor
  @Getter
  public static class Result {
    private final Patient patient;
    private final boolean isPatientNew;
    private final Person person;
    private final boolean isPersonNew;
  }
  
}
