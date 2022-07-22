package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.ClinicalImpression;
import org.hl7.fhir.r4.model.Patient;

@AllArgsConstructor
public class ClinicalImpressionBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final Patient patient;
  
  public Result build() {
    final ClinicalImpression clinicalImpression = mapper.mapToClinicalImpression(patient);
    return new Result(clinicalImpression);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ClinicalImpression clinicalImpression;
  }
}
