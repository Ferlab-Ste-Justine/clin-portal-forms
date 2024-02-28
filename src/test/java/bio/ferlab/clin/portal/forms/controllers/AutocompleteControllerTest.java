package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.autocomplete.Supervisor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import ca.uhn.fhir.context.FhirContext;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class AutocompleteControllerTest {

  // was just a PoC to see how easy it's to Start an entire spring boot context and mock some services

  private final FhirContext fhirContext = FhirContext.forR4();
  private final Algorithm algorithm = Algorithm.HMAC256("secret");

  @MockBean
  private FhirClient fhirClient;

  @Autowired
  private AutocompleteController controller;

  @BeforeEach
  void beforeEach() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }

  @Test
  void supervisor() {

    final Bundle epBundle = new Bundle();
    PractitionerRole roleEp = new PractitionerRole();
    roleEp.setOrganization(new Reference("Organization/ep"));
    epBundle.addEntry().setResource(roleEp);

    final Bundle rolesBundle = new Bundle();
    final Practitioner p1 = new Practitioner();
    p1.setId("p1");
    p1.getNameFirstRep().setFamily("name");
    final Identifier identifier = new Identifier().setValue("1234");
    final List<Identifier> identifiers = new ArrayList<>(Collections.singletonList(identifier));
//    identifiers.add(identifier);
    p1.setIdentifier(identifiers);
    final PractitionerRole r1 = new PractitionerRole();
    r1.setId("r1");
    r1.setPractitioner(FhirUtils.toReference(p1));
    r1.getCodeFirstRep().getCodingFirstRep().setCode("doctor");
    r1.getOrganization().setReference("Organization/ep");
    rolesBundle.addEntry().setResource(p1);
    rolesBundle.addEntry().setResource(r1);

    when(fhirClient.findPractitionerRoleByPractitionerId(any())).thenReturn(epBundle);
    when(fhirClient.findPractitionerAndRoleByEp(any())).thenReturn(rolesBundle);

    final String token = JWT.create().withClaim(JwtUtils.FHIR_PRACTITIONER_ID, "practitionerId").sign(algorithm);
    final List<Supervisor> results = controller.autocomplete("Bearer "+token, "ep", "name");

    verify(fhirClient).findPractitionerRoleByPractitionerId(eq("practitionerId"));
    verify(fhirClient).findPractitionerAndRoleByEp(eq("ep"));

    assertEquals(1, results.size());
    assertEquals("r1", results.get(0).getId());
    assertEquals("1234", results.getFirst().getLicense());
  }
}
