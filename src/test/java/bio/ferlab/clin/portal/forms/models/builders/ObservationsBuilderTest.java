package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.*;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;
import static org.junit.jupiter.api.Assertions.*;

class ObservationsBuilderTest {

  final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandom();

  @Test
  void validate_empty_age_code() {
    final List<Signs> signs = random.objects(Signs.class, 5).collect(Collectors.toList());
    signs.get(2).setIsObserved(true);
    signs.get(2).setAgeCode(null);
    final ClinicalSigns clinicalSigns = new ClinicalSigns();
    clinicalSigns.setSigns(signs);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new ObservationsBuilder(null, null, null, null, clinicalSigns, new ParaclinicalExams()).validate();
    });
    assertEquals("age_code can't be empty for clinical_signs[2].is_observed = true", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void validate_empty_values() {
    final List<Exams> exams = random.objects(Exams.class, 5).collect(Collectors.toList());
    exams.get(3).setInterpretation(Exams.Interpretation.abnormal);
    exams.get(3).setValue(null);
    exams.get(3).setValues(null);
    final ParaclinicalExams paraclinicalExams = new ParaclinicalExams();
    paraclinicalExams.setExams(exams);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new ObservationsBuilder(null, null, null, null, new ClinicalSigns(), paraclinicalExams).validate();
    });
    assertEquals("value and values can't be both empty for paraclinical_exams[3].interpretation = abnormal", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void build() {
    final Patient patient = new Patient();
    patient.setId("foo");
    final HistoryAndDiag historyAndDiag = random.nextObject(HistoryAndDiag.class);
    historyAndDiag.setInbreeding(false);

    Signs cs1 = new Signs();
    cs1.setIsObserved(true);
    cs1.setAgeCode("age");
    cs1.setValue("sign");

    Signs cs2 = new Signs();
    cs2.setIsObserved(false);
    cs2.setValue("sign");

    final List<Signs> signs = List.of(cs1, cs2);

    Exams ex1 = new Exams();
    ex1.setCode("code1");
    ex1.setInterpretation(Exams.Interpretation.normal);

    Exams ex2 = new Exams();
    ex2.setCode("code2");
    ex2.setValue("value");
    ex2.setInterpretation(Exams.Interpretation.abnormal);

    Exams ex3 = new Exams();
    ex3.setCode("code3");
    ex3.setValues(List.of("value1", "value2"));
    ex3.setInterpretation(Exams.Interpretation.abnormal);

    Exams ex4 = new Exams();  // should be ignored
    ex4.setInterpretation(Exams.Interpretation.not_done);

    final List<Exams> exams = List.of(ex1, ex2, ex3, ex4);

    final ClinicalSigns clinicalSigns = new ClinicalSigns();
    clinicalSigns.setSigns(signs);
    clinicalSigns.setComment("foo");

    final ParaclinicalExams paraclinicalExams = new ParaclinicalExams();
    paraclinicalExams.setExams(exams);
    paraclinicalExams.setComment("bar");

    final Observation foetusObservation = new Observation();
    foetusObservation.setId("foetusObs");

    final Parent mother = new Parent();
    mother.setParentEnterMoment(Parent.Moment.now); // ignored
    mother.setParentClinicalStatus(Parent.Status.unknown);

    final Parent father = new Parent();
    father.setParentEnterMoment(Parent.Moment.later);

    final ObservationsBuilder.Result result =
        new ObservationsBuilder(new SubmitToFhirMapper(), "code", patient, historyAndDiag, clinicalSigns, paraclinicalExams)
        .withFoetus(foetusObservation)
        .withParentAffected(mother)
        .withMother(mother)
        .withFather(father)
        .build();

    final List<Observation> obs = result.getObservations();

    assertEquals(13, obs.size());

    assertObservation(obs.get(0), patient, "DSTA", "exam", ObservationsBuilder.Affected.IND, ANALYSIS_REQUEST_CODE, "code", true);


    assertObservation(obs.get(1), patient, "PHEN", "exam", ObservationsBuilder.Affected.POS, HP_CODE, "sign", true);
    assertEquals("age", ((Coding)obs.get(1).getExtensionByUrl(AGE_AT_ONSET_EXT).getValue()).getCode());

    assertObservation(obs.get(2), patient, "PHEN", "exam", ObservationsBuilder.Affected.NEG, HP_CODE, "sign", true);
    assertNull(obs.get(2).getExtensionByUrl(AGE_AT_ONSET_EXT));

    assertObservation(obs.get(3), patient, "OBSG", "exam", null, null, clinicalSigns.getComment(), true);

    assertObservation(obs.get(4), patient, "code1", "procedure", null, null, null, false);
    assertEquals(OBSERVATION_INTERPRETATION,obs.get(4).getInterpretationFirstRep().getCodingFirstRep().getSystem());
    assertEquals("N",obs.get(4).getInterpretationFirstRep().getCodingFirstRep().getCode());

    assertObservation(obs.get(5), patient, "code2", "procedure", null, null, "value", false);
    assertEquals(OBSERVATION_INTERPRETATION,obs.get(5).getInterpretationFirstRep().getCodingFirstRep().getSystem());
    assertEquals("A",obs.get(5).getInterpretationFirstRep().getCodingFirstRep().getCode());

    assertObservation(obs.get(6), patient, "code3", "procedure", null, null, null, false);
    assertEquals(OBSERVATION_INTERPRETATION,obs.get(6).getInterpretationFirstRep().getCodingFirstRep().getSystem());
    assertEquals("A",obs.get(6).getInterpretationFirstRep().getCodingFirstRep().getCode());
    assertEquals(HP_CODE,obs.get(6).getValueCodeableConcept().getCoding().get(0).getSystem());
    assertEquals("value1",obs.get(6).getValueCodeableConcept().getCoding().get(0).getCode());
    assertEquals(HP_CODE,obs.get(6).getValueCodeableConcept().getCoding().get(1).getSystem());
    assertEquals("value2",obs.get(6).getValueCodeableConcept().getCoding().get(1).getCode());

    assertObservation(obs.get(7), patient, "INVES", "exam", null, null, paraclinicalExams.getComment(), true);

    assertObservation(obs.get(8), patient, "INDIC", "exam", null, null, historyAndDiag.getDiagnosticHypothesis(), true);

    assertObservation(obs.get(9), patient, "ETHN", "exam", null, ETHNICITY_CODE, historyAndDiag.getEthnicity(), true);

    assertObservation(obs.get(10), patient, "CONS", "exam", null, null, historyAndDiag.getInbreeding(), true);

    assertObservation(obs.get(11), patient, "MFTH", "social-history", null, SYSTEM_MISSING_PARENT, CODE_MISSING_PARENT_TEMPORARY, false);

    assertEquals("foetusObs",obs.get(12).getId());
 }

  private void assertObservation(Observation obs, Patient patient, String code, String category, ObservationsBuilder.Affected affected, String system, Object value, boolean checkInterpretation) {
    assertNotNull(obs.getId());
    assertEquals(FhirUtils.formatResource(patient), obs.getSubject().getReference());
    assertEquals(OBSERVATION_CODE, obs.getCode().getCodingFirstRep().getSystem());
    assertEquals(code, obs.getCode().getCodingFirstRep().getCode());
    assertEquals(OBSERVATION_PROFILE, obs.getMeta().getProfile().get(0).getValue());
    assertEquals(Observation.ObservationStatus.FINAL, obs.getStatus());
    assertEquals(OBSERVATION_CATEGORY, obs.getCategoryFirstRep().getCodingFirstRep().getSystem());
    assertEquals(category, obs.getCategoryFirstRep().getCodingFirstRep().getCode());
    if (affected != null) {
      assertEquals(OBSERVATION_INTERPRETATION, obs.getInterpretationFirstRep().getCodingFirstRep().getSystem());
      assertEquals(affected.name(), obs.getInterpretationFirstRep().getCodingFirstRep().getCode());
    } else if (checkInterpretation) {
      assertEquals(0, obs.getInterpretation().size());
    }
    if (value != null) {
      if (value instanceof String) {
        if (system != null) {
          assertEquals(system, obs.getValueCodeableConcept().getCodingFirstRep().getSystem());
          assertEquals(value, obs.getValueCodeableConcept().getCodingFirstRep().getCode());
        } else {
          assertEquals(value, obs.getValueStringType().getValue());
        }
      }else if(value instanceof Boolean) {
        assertEquals(value, obs.getValueBooleanType().getValue());
      }
    }
  }

  @Test
  void shouldBeRobustWithParentStatus() {
    assertNotNull(new ObservationsBuilder(new SubmitToFhirMapper(), null, null, null, null, null)
        .withParentAffected(null)
        .build());
    assertNotNull(new ObservationsBuilder(new SubmitToFhirMapper(), null, null, null, null, null)
      .withParentAffected(new Parent())
      .build());
    final Parent parent = new Parent();
    parent.setParentClinicalStatus(Parent.Status.affected);
    var result = new ObservationsBuilder(new SubmitToFhirMapper(), null, new Patient(), null, null, null)
      .withParentAffected(parent)
      .build();
    assertEquals(1, result.getObservations().size());
  }

}
