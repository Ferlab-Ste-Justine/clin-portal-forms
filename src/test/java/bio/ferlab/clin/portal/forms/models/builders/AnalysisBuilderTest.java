package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalysisBuilderTest {

  @Test
  void build() {
    final Patient patient = new Patient();
    patient.setId("foo");
    
    final ClinicalImpression clinicalImpression = new ClinicalImpression();
    clinicalImpression.setId("foo");
    
    final Practitioner practitioner = new Practitioner();
    practitioner.setId("foo");
    
    final PractitionerRole role = new PractitionerRole();
    role.setId("foo");
    role.setPractitioner(FhirUtils.toReference(practitioner));
    
    final PractitionerRole supervisor = new PractitionerRole();
    supervisor.setId("foo");
    
    final AnalysisBuilder.Result result = new AnalysisBuilder(new SubmitToFhirMapper(), "code", patient, clinicalImpression, role, supervisor, "")
      .withFoetus(new Patient())
      .withReflex(true)
      .withMother(new Patient())
      .build();
    final ServiceRequest sr = result.getAnalysis();

    assertNotNull(sr.getId());
    assertEquals(ANALYSIS_SERVICE_REQUEST, sr.getMeta().getProfile().get(0).getValue());
    assertEquals(ServiceRequest.ServiceRequestIntent.ORDER, sr.getIntent());
    assertEquals(FhirUtils.formatResource(patient), sr.getSubject().getReference());
    assertEquals(ServiceRequest.ServiceRequestStatus.ONHOLD, sr.getStatus());
    assertEquals(FhirUtils.formatResource(clinicalImpression), sr.getSupportingInfoFirstRep().getReference());
    assertEquals("code", sr.getCode().getCodingFirstRep().getCode());
    assertNotNull(sr.getAuthoredOn());
    assertEquals(FhirUtils.formatResource(role), sr.getRequester().getReference());
    assertEquals(FhirUtils.formatResource(supervisor), ((Reference)sr.getExtensionByUrl(SUPERVISOR_EXT).getValue()).getReference());
    assertEquals("Reflex Panel: Global Muscular diseases (MMG)", sr.getOrderDetailFirstRep().getText());
    final Annotation note = sr.getNoteFirstRep();
    assertNotNull(note.getTime());
    assertEquals("--", note.getText());
    assertEquals(FhirUtils.formatResource(practitioner), ((Reference)note.getAuthor()).getReference());
    assertEquals("Prenatal", sr.getCategoryFirstRep().getText());

    final Extension motherRootExt = sr.getExtensionByUrl(FAMILY_MEMBER);
    final Extension motherSub1Ext = motherRootExt.getExtension().get(0);
    final Extension motherSub2Ext = motherRootExt.getExtension().get(1);
    assertEquals("parent", motherSub1Ext.getUrl());
    assertEquals("Patient/null", ((Reference) motherSub1Ext.getValue()).getReference());
    assertEquals("parent-relationship", motherSub2Ext.getUrl());
    assertEquals(SYSTEM_ROLE, ((CodeableConcept) motherSub2Ext.getValue()).getCodingFirstRep().getSystem());
    assertEquals("MTH", ((CodeableConcept) motherSub2Ext.getValue()).getCodingFirstRep().getCode());
  }
}