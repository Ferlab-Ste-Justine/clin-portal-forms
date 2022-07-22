package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.ClinicalImpression;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;

@AllArgsConstructor
public class AnalysisBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final String panelCode;
  private final Patient patient;
  private ClinicalImpression clinicalImpression;

  public Result build() {
    final ServiceRequest serviceRequest = mapper.mapToAnalysis(panelCode, patient, clinicalImpression);
    return new Result(serviceRequest);
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest analysis;
  }
}
