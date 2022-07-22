package bio.ferlab.clin.portal.forms.clients;

import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Component
@Getter
@Slf4j
public class FhirClient {
  
  private final FhirContext context;
  private final IGenericClient genericClient;
  
  public FhirClient(FhirConfiguration configuration, FhirAuthInterceptor fhirAuthInterceptor) {
    context = FhirContext.forR4();
    context.getRestfulClientFactory().setConnectTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setSocketTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setPoolMaxTotal(configuration.getPoolSize());
    context.getRestfulClientFactory().setPoolMaxPerRoute(configuration.getPoolSize());
    context.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
    context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

    this.genericClient = context.newRestfulGenericClient(configuration.getUrl());
    
    genericClient.registerInterceptor(fhirAuthInterceptor);
  }

  public void validate(IBaseResource resource) {
    List<String> errors = new ArrayList<>();
    OperationOutcome oo;
    try {
      oo = (OperationOutcome) getGenericClient().validate().resource(resource).encodedJson().execute().getOperationOutcome();
    } catch(PreconditionFailedException | UnprocessableEntityException e) {
      oo = (OperationOutcome) e.getOperationOutcome();
    }
    for (OperationOutcome.OperationOutcomeIssueComponent nextIssue : oo.getIssue()) {
      if (EnumSet.of(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueSeverity.FATAL).contains(nextIssue.getSeverity())) {
        errors.add(nextIssue.getDiagnostics());
      } else if (OperationOutcome.IssueSeverity.WARNING.equals(nextIssue.getSeverity())){
        log.warn("Validation of resource {} : {}", FhirUtils.formatResource(resource), nextIssue.getDiagnostics());
      }
    }

    if (!errors.isEmpty()) {
      throw new RuntimeException("Validation of resource "+ FhirUtils.formatResource(resource)+" errors:\n" + StringUtils.join(errors, "\n"));
    }
  }

  public void logDebug(IBaseResource resource) {
    log.debug("JSON of {}\n{}", FhirUtils.formatResource(resource), getContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource));
  }
  
}
