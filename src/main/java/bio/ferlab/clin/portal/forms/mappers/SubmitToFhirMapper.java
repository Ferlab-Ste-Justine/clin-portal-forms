package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.builders.ObservationsBuilder;
import bio.ferlab.clin.portal.forms.models.submit.Patient;
import bio.ferlab.clin.portal.forms.models.submit.*;
import bio.ferlab.clin.portal.forms.utils.DateUtils;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@Component
public class SubmitToFhirMapper {

  public long mapToAge(Date birthDate) {
    // ChronoUnit.YEARS doesn't work, in case you want to use it ...
    return ChronoUnit.DAYS.between(birthDate.toInstant(), Instant.now());
  }

  public Enumerations.AdministrativeGender mapToGender(Patient.Gender gender) {
    return Enumerations.AdministrativeGender.fromCode(gender.name());
  }

  public Person mapToPerson(Patient patient, org.hl7.fhir.r4.model.Patient linkedPatient) {
    final Person p = new Person();
    // don't use IdType.newRandomUuid() because it creates a conflict when looking for Person/urn:uuid:xxxx
    p.setId(UUID.randomUUID().toString());
    this.updatePerson(patient, p, linkedPatient);
    return p;
  }

  public org.hl7.fhir.r4.model.Patient mapToPatient(Patient patient) {
    final Reference epRef = FhirUtils.toReference(new Organization().setId(patient.getEp()));
    final org.hl7.fhir.r4.model.Patient p = new org.hl7.fhir.r4.model.Patient();
    p.setId(UUID.randomUUID().toString());
    p.setManagingOrganization(epRef);
    this.updatePatient(patient, p);
    return p;
  }

  public void updatePatient(Patient patient, org.hl7.fhir.r4.model.Patient res) {
    final Reference epRef = FhirUtils.toReference(new Organization().setId(patient.getEp()));
    res.setGender(mapToGender(patient.getGender()));
    updateIdentifier(res.getIdentifier(), SYSTEM_MRN, CODE_MRN, patient.getMrn(), epRef);
  }

  public void updatePerson(Patient patient, Person person, org.hl7.fhir.r4.model.Patient linkedPatient) {
    updateIdentifier(person.getIdentifier(), SYSTEM_RAMQ, CODE_RAMQ, Utils.removeSpaces(patient.getRamq()), null);
    person.setBirthDate(DateUtils.toDate(patient.getBirthDate()));
    person.setGender(mapToGender(patient.getGender()));
    person.getName().clear();
    person.addName().addGiven(patient.getFirstName()).setFamily(patient.getLastName());
    final String linkedPatientRef = FhirUtils.formatResource(linkedPatient);
    boolean isLinked = person.getLink().stream().anyMatch(l -> linkedPatientRef.equals(l.getTarget().getReference()));
    if(!isLinked){
      person.getLink().add(new Person.PersonLinkComponent(new Reference(linkedPatientRef)));
    }
  }

  public ServiceRequest mapToAnalysis(String panelCode, org.hl7.fhir.r4.model.Patient patient,
                                      ClinicalImpression clinicalImpression,
                                      ClinicalImpression clinicalImpressionMother,
                                      ClinicalImpression clinicalImpressionFather,
                                      String orderDetails, PractitionerRole practitionerRole,
                                      PractitionerRole supervisorRole,
                                      String comment, org.hl7.fhir.r4.model.Patient foetus) {
    final ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setId(UUID.randomUUID().toString());
    if (foetus != null) {
      serviceRequest.addCategory().setText(PRENATAL);
      mapDeceased(serviceRequest, foetus);
    }
    serviceRequest.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
    serviceRequest.setSubject(FhirUtils.toReference(patient));
    serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ONHOLD);
    serviceRequest.addSupportingInfo(FhirUtils.toReference(clinicalImpression));
    serviceRequest.setCode(new CodeableConcept().addCoding(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode(panelCode)));
    serviceRequest.setAuthoredOn(new Date());
    serviceRequest.setRequester(FhirUtils.toReference(practitionerRole));
    String sanitizedComment = FhirUtils.sanitizeNoteComment(comment);
    serviceRequest.addNote(new Annotation().setText(sanitizedComment).setTime(new Date()).setAuthor(practitionerRole.getPractitioner()));
    if (StringUtils.isNotBlank(orderDetails)) {
      serviceRequest.addOrderDetail(new CodeableConcept().setText(orderDetails));
    }
    if (supervisorRole != null) {
      serviceRequest.addExtension(SUPERVISOR_EXT, FhirUtils.toReference(supervisorRole));
    }
    this.addParent(serviceRequest, clinicalImpressionMother, FAMILY_MEMBER_MOTHER_CODE);
    this.addParent(serviceRequest, clinicalImpressionFather, FAMILY_MEMBER_FATHER_CODE);
    return serviceRequest;
  }

  public ServiceRequest mapToSequencing(String panelCode, org.hl7.fhir.r4.model.Patient patient, ServiceRequest analysis, PractitionerRole practitionerRole, org.hl7.fhir.r4.model.Patient foetus) {
    final ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setId(UUID.randomUUID().toString());
    serviceRequest.getMeta().addProfile(SEQUENCING_SERVICE_REQUEST);
    serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
    if (foetus != null) {
      serviceRequest.addCategory().setText(PRENATAL);
      serviceRequest.setSubject(FhirUtils.toReference(foetus));
      mapDeceased(serviceRequest, foetus);
    } else {
      serviceRequest.setSubject(FhirUtils.toReference(patient));
    }
    serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ONHOLD);
    serviceRequest.addBasedOn(FhirUtils.toReference(analysis));
    serviceRequest.setRequester(FhirUtils.toReference(practitionerRole));
    serviceRequest.setCode(new CodeableConcept().addCoding(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode(panelCode)));
    serviceRequest.getCode().addCoding(new Coding().setSystem(SEQUENCING_REQUEST_CODE).setCode("75020"));
    serviceRequest.setAuthoredOn(new Date());
    return serviceRequest;
  }

  private void mapDeceased(ServiceRequest serviceRequest, org.hl7.fhir.r4.model.Patient foetus) {
    if (foetus.hasDeceased()) {
      if (((BooleanType) foetus.getDeceased()).booleanValue()) {
        serviceRequest.setPriority(ServiceRequest.ServiceRequestPriority.ROUTINE);
      } else {
        serviceRequest.setPriority(ServiceRequest.ServiceRequestPriority.ASAP);
      }
    }
  }

  public ClinicalImpression mapToClinicalImpression(Person person, org.hl7.fhir.r4.model.Patient patient,
                                                    List<Observation> observations,
                                                    List<FamilyMemberHistory> histories) {
    final ClinicalImpression clinicalImpression = new ClinicalImpression();
    clinicalImpression.setId(UUID.randomUUID().toString());
    clinicalImpression.setSubject(FhirUtils.toReference(patient));
    clinicalImpression.setStatus(ClinicalImpression.ClinicalImpressionStatus.COMPLETED);
    clinicalImpression.addExtension(AGE_AT_EVENT_EXT, new Age().setSystem(Enumerations.AgeUnits.D.getSystem()).setCode(Enumerations.AgeUnits.D.toCode()).setValue(mapToAge(person.getBirthDate())));
    clinicalImpression.addInvestigation(new ClinicalImpression.ClinicalImpressionInvestigationComponent(new CodeableConcept().setText("Examination / signs")));
    observations.forEach(o -> clinicalImpression.getInvestigationFirstRep().addItem(FhirUtils.toReference(o)));
    histories.forEach(o -> clinicalImpression.getInvestigationFirstRep().addItem(FhirUtils.toReference(o)));
    return clinicalImpression;
  }

  public List<Observation> mapToObservations(String panelCode,
                                             org.hl7.fhir.r4.model.Patient patient,
                                             Parent mother, Parent father,
                                             HistoryAndDiag historyAndDiag,
                                             ClinicalSigns signs,
                                             ParaclinicalExams exams, ObservationsBuilder.Affected affected) {

    List<Observation> all = new ArrayList<>();

    Observation dsta = createObservation(patient, "DSTA", "exam", affected, ANALYSIS_REQUEST_CODE, panelCode);
    all.add(dsta);

    if (signs != null) {
      all.addAll(signs.getSigns().stream().map(o -> {
        Observation obs = createObservation(patient, "PHEN", "exam", FhirUtils.toAffected(o.getIsObserved()), HP_CODE, o.getValue());
        if (o.getAgeCode() != null) {
          obs.addExtension(AGE_AT_ONSET_EXT, new Coding().setCode(o.getAgeCode()));
        }
        return obs;
      }).toList());

      if(StringUtils.isNotBlank(signs.getComment())) {
        Observation obsg = createObservation(patient, "OBSG", "exam",null, null, signs.getComment());
        all.add(obsg);
      }
    }

    if (exams != null) {
      all.addAll(exams.getExams().stream()
        .filter(o -> EnumSet.of(Exams.Interpretation.normal, Exams.Interpretation.abnormal).contains(o.getInterpretation()))
        .map(o -> {
          Observation obs = createObservation(patient, o.getCode(), "procedure", null, null, o.getValue());
          obs.addInterpretation(new CodeableConcept(new Coding().setSystem(OBSERVATION_INTERPRETATION).setCode(getInterpretationCode(o.getInterpretation()))));
          o.getValues().forEach(v -> obs.getValueCodeableConcept().addCoding(new Coding().setSystem(HP_CODE).setCode(v)));
          return obs;
        }).toList());

      if(StringUtils.isNotBlank(exams.getComment())) {
        Observation obsg = createObservation(patient, "INVES", "exam", null, null, exams.getComment());
        all.add(obsg);
      }
    }


    if (historyAndDiag !=null) {
      Observation indic = createObservation(patient, "INDIC", "exam", null, null, historyAndDiag.getDiagnosticHypothesis());
      all.add(indic);

      if (StringUtils.isNotBlank(historyAndDiag.getEthnicity())) {
        Observation obsg = createObservation(patient, "ETHN", "exam",null, ETHNICITY_CODE, historyAndDiag.getEthnicity());
        all.add(obsg);
      }

      if (historyAndDiag.getInbreeding() != null) {
        Observation cons = createObservation(patient, "CONS", "exam", null, null, historyAndDiag.getInbreeding());
        all.add(cons);
      }
    }

    addParentObservation(all, patient, mother, "MMTH");
    addParentObservation(all, patient, father, "MFTH");

    return all;
  }

  public List<FamilyMemberHistory> mapToFamilyMemberHistory(HistoryAndDiag historyAndDiag, org.hl7.fhir.r4.model.Patient patient) {
    final List<FamilyMemberHistory> histories = new ArrayList<>();
    for(HealthCondition healthCondition: historyAndDiag.getHealthConditions()) {
      final FamilyMemberHistory history = new FamilyMemberHistory();
      history.setId(UUID.randomUUID().toString());
      history.setStatus(FamilyMemberHistory.FamilyHistoryStatus.COMPLETED);
      history.getRelationship().addCoding().setSystem(SYSTEM_RELATIONSHIP).setCode(healthCondition.getParentalLink());
      String sanitizedComment = FhirUtils.sanitizeNoteComment(healthCondition.getCondition());
      history.addNote(new Annotation().setText(sanitizedComment));
      history.setPatient(FhirUtils.toReference(patient));
      histories.add(history);
    }
    return histories;
  }

  public RelatedPerson mapToRelatedPerson(org.hl7.fhir.r4.model.Patient patient, String motherRamq) {
    final RelatedPerson relatedPerson = new RelatedPerson();
    relatedPerson.setId(UUID.randomUUID().toString());
    this.updateIdentifier(relatedPerson.getIdentifier(), SYSTEM_RAMQ, CODE_RAMQ, motherRamq, null);
    relatedPerson.setPatient(FhirUtils.toReference(patient));
    relatedPerson.addRelationship().setText("Mother").addCoding().setSystem(SYSTEM_ROLE).setCode(ROLE_CODE_MOTHER);
    relatedPerson.setActive(true);
    return relatedPerson;
  }

  public org.hl7.fhir.r4.model.Patient mapToFoetus(AdditionalInfo additionalInfo, org.hl7.fhir.r4.model.Patient mother) {
    final org.hl7.fhir.r4.model.Patient foetus = new org.hl7.fhir.r4.model.Patient();
    foetus.setId(UUID.randomUUID().toString());
    //foetus.addExtension(GESTATIONAL_AGE_EXT, new CodeableConcept(new Coding().setSystem(SYSTEM_GESTATIONAL_AGE).setCode(CODE_GESTATIONAL_AGE)).setText(additionalInfo.getGestationalAge().name()));
    foetus.setDeceased(new BooleanType(AdditionalInfo.GestationalAge.deceased.equals(additionalInfo.getGestationalAge())));
    foetus.setGender(mapToGender(additionalInfo.getFoetusGender()));
    final var link = new org.hl7.fhir.r4.model.Patient.PatientLinkComponent();
    link.setOther(FhirUtils.toReference(mother));
    link.setType(org.hl7.fhir.r4.model.Patient.LinkType.SEEALSO);
    foetus.setManagingOrganization(mother.getManagingOrganization()); // required for metatag
    foetus.addLink(link);
    return foetus;
  }

  public Observation mapToObservation(AdditionalInfo additionalInfo, org.hl7.fhir.r4.model.Patient mother, org.hl7.fhir.r4.model.Patient foetus) {
    final Observation observation = new Observation();
    observation.setId(UUID.randomUUID().toString());
    observation.setStatus(Observation.ObservationStatus.FINAL);
    observation.setSubject(FhirUtils.toReference(mother));
    observation.setFocus(List.of(FhirUtils.toReference(foetus)));
    switch (additionalInfo.getGestationalAge()) {
      case ddm:
        observation.setCode(new CodeableConcept(new Coding().setSystem(SYSTEM_DDM).setCode(CODE_DDM).setDisplay(DISPLAY_DDM)));
        break;
      case dpa:
        observation.setCode(new CodeableConcept(new Coding().setSystem(SYSTEM_DPA).setCode(CODE_DPA).setDisplay(DISPLAY_DPA)));
        break;
      default:
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info gestational_age should be ddm or dpa");
    }
    observation.getValueDateTimeType().setValue(DateUtils.toDate(additionalInfo.getGestationalDate()));
    return observation;
  }

  private void addParent(ServiceRequest serviceRequest, ClinicalImpression clinicalImpressionParent, String code) {
    if (clinicalImpressionParent != null) {
      // add clinical impression
      serviceRequest.addSupportingInfo(FhirUtils.toReference(clinicalImpressionParent));
      // add family member extension
      final Extension familyMemberExt = new Extension(FAMILY_MEMBER);
      final Extension parentExt = new Extension("parent");
      parentExt.setValue(clinicalImpressionParent.getSubject());
      final Extension parentRelationExt = new Extension("parent-relationship");
      final CodeableConcept cc = new CodeableConcept();
      cc.getCodingFirstRep().setSystem(SYSTEM_ROLE).setCode(code);
      parentRelationExt.setValue(cc);
      familyMemberExt.addExtension(parentExt);
      familyMemberExt.addExtension(parentRelationExt);
      serviceRequest.addExtension(familyMemberExt);
    }
  }

  private void addParentObservation(List<Observation> observations, org.hl7.fhir.r4.model.Patient patient, Parent parent, String code) {
    if (parent != null && EnumSet.of(Parent.Moment.later, Parent.Moment.never).contains(parent.getParentEnterMoment())) {
      final Observation obs = createObservation(patient, code, "social-history", null, SYSTEM_MISSING_PARENT, Parent.Moment.later.equals(parent.getParentEnterMoment()) ? CODE_MISSING_PARENT_TEMPORARY : CODE_MISSING_PARENT_PERMANENT);
      obs.setEffective(new Period().setStart(DateUtils.toDate(LocalDate.now())));
      String sanitizedComment = FhirUtils.sanitizeNoteComment(parent.getParentNoInfoReason());
      obs.addNote(new Annotation().setText(sanitizedComment));
      observations.add(obs);
    }
  }

  private String getInterpretationCode(Exams.Interpretation interpretation) {
    switch (interpretation){
      case abnormal:
        return "A";
      case normal:
        return "N";
      default:
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid interpretation value: " + interpretation);
    }
  }

  private Observation createObservation(org.hl7.fhir.r4.model.Patient patient, String code, String category, ObservationsBuilder.Affected affected, String system, Object value) {
    Observation observation = new Observation();
    observation.setSubject(FhirUtils.toReference(patient));
    observation.setId(UUID.randomUUID().toString());
    observation.getMeta().addProfile(OBSERVATION_PROFILE);
    observation.setStatus(Observation.ObservationStatus.FINAL);
    observation.setCode(new CodeableConcept(new Coding().setSystem(OBSERVATION_CODE).setCode(code)));
    observation.addCategory(new CodeableConcept(new Coding().setSystem(OBSERVATION_CATEGORY).setCode(category)));
    if(affected != null) {
      observation.addInterpretation(new CodeableConcept(new Coding().setSystem(OBSERVATION_INTERPRETATION).setCode(affected.name())));
    }
    if (value instanceof String) {
      final String valueStr = value.toString();
      if (StringUtils.isNotBlank(valueStr)) {
        if (StringUtils.isNotBlank(system)) {
          observation.setValue(new CodeableConcept(new Coding().setSystem(system).setCode(valueStr)));
        } else {
          observation.setValue(new StringType(valueStr));
        }
      }
    } else if (value instanceof Boolean b) {
      observation.setValue(new BooleanType(b));
    }
    return observation;
  }

  private void updateIdentifier(List<Identifier> identifiers, String system, String code, String value, Reference assigner){
    if(StringUtils.isNotBlank(value)) {
      Identifier identifierToUpdate = null;
      for(Identifier identifier: identifiers) {
        if (identifier.hasType() && identifier.getType().hasCoding()) {
          for (Coding coding : identifier.getType().getCoding()) {
            if (system.equals(coding.getSystem()) && code.equals(coding.getCode())) {
              identifierToUpdate = identifier;
            }
          }
        }
      }
      if (identifierToUpdate != null) {
        final String previousValue = identifierToUpdate.getValue();
        if (StringUtils.isBlank(previousValue) || !previousValue.equals(value)) {
          identifierToUpdate.setValue(value); // FHIR Server will check if modifying MRN/RAMQ is allowed or not
        }
      } else {
        identifiers.add(new Identifier()
            .setValue(value)
            .setAssigner(assigner)
            .setType(new CodeableConcept().addCoding(new Coding().setSystem(system).setCode(code))));
      }
    }
  }
}
