package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CodesValuesServiceTest {

  private final FhirContext fhirContext = FhirContext.forR4();
  private final FhirClient fhirClient = Mockito.mock(FhirClient.class);
  private final FhirConfiguration fhirConfiguration = Mockito.mock(FhirConfiguration.class);
  private final LogOnceService logOnceService = Mockito.mock(LogOnceService.class);
  private final ApplicationContext applicationContext = Mockito.mock(MockApplicationContext.class);
  private final CodesValuesService service =new CodesValuesService(fhirClient, fhirConfiguration, logOnceService, applicationContext);

  @BeforeEach
  void beforeEach() {

    when(applicationContext.getBean(eq(CodesValuesService.class))).thenReturn(service);

    final Bundle bundle = new Bundle();

    var hp = new CodeSystem();
    hp.addConcept().setCode("code0");

    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(hp);
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new ValueSet());
    bundle.addEntry().setResource(new ValueSet());

    var type1 = new ValueSet();
    type1.getCompose().getIncludeFirstRep().addConcept().setCode("code1");
    bundle.addEntry().setResource(type1);
    bundle.addEntry().setResource(new ValueSet());

    when(fhirClient.getContext()).thenReturn(fhirContext);
    when(fhirClient.fetchCodesAndValues()).thenReturn(bundle);
    when(fhirConfiguration.getTypesWithDefault()).thenReturn(List.of("type1"));
    when(fhirConfiguration.getMultiValuesObservationCodes()).thenReturn(List.of());
  }

  @Test
  void getCodes() {
    final CodeSystem res = this.service.getCodes(CodesValuesService.ANALYSE_KEY);
    assertNotNull(res);
  }

  @Test
  void getValues() {
    final ValueSet res = this.service.getValues(CodesValuesService.AGE_KEY);
    assertNotNull(res);
  }

  @Test
  void getHPOByCode() {
    assertNull(service.getHPOByCode(null));
    assertNull(service.getHPOByCode("foo"));
    assertNotNull(service.getHPOByCode("code1"));
    assertNotNull(service.getHPOByCode("code0"));
  }

  // dont ask me why ... mockito doesnt like to mock interface
  private static abstract class MockApplicationContext implements ApplicationContext {}
}
