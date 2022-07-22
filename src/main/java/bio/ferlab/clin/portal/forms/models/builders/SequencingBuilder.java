package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;

@AllArgsConstructor
public class SequencingBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final String panelCode;
  private final Patient patient;
  private final ServiceRequest analysis;
  
  public Result build() {
    final ServiceRequest serviceRequest = mapper.mapToSequencing(panelCode, patient, analysis);
    return new Result(serviceRequest);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest sequencing;
  }
}
