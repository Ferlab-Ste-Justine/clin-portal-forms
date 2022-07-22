package bio.ferlab.clin.portal.forms.utils;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Reference;

public class FhirUtils {
  
  public static String formatResource(IBaseResource resource) {
    return String.format("%s/%s", resource.fhirType(), resource.getIdElement().getIdPart());
  }

  public static Reference toReference(IBaseResource resource) {
    return new Reference(formatResource(resource));
  }
}
