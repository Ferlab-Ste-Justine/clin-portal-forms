package bio.ferlab.clin.portal.forms.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.util.ArrayList;
import java.util.List;

public class BundleExtractor {

  private final FhirContext fhirContext;
  private final IBaseBundle bundle;
  private int currentIndex = 0;
  
  public BundleExtractor(FhirContext fhirContext, IBaseBundle bundle)  {
    this.fhirContext = fhirContext;
    this.bundle = bundle;
  }

  public <T extends IBaseResource> List<T> getAllResourcesOfType(Class<T> clazz) {
    List<T> results = new ArrayList<>();
    List<IBaseResource> allRes = BundleUtil.toListOfResources(fhirContext, bundle);
    for(IBaseResource res : allRes) {
      if(res.getClass().equals(clazz)){
        results.add((T)res);
      }
    }
    return results;
  }
  
  public <T extends IBaseResource> List<T> getNextListOfResourcesOfType(Class<T> clazz) {
    return getListOfResourcesOfTypeAtIndex(fhirContext, bundle, currentIndex++, clazz);
  }
  
  public <T extends IBaseResource> T getNextResourcesOfType(Class<T> clazz) {
    return getResourcesOfTypeAtIndex(fhirContext, bundle, currentIndex++, clazz);
  }

  public <T extends IBaseResource> List<T> getListOfResourcesOfTypeAtIndex(FhirContext context, IBaseBundle bundle, int indexInBundle, Class<T> clazz) {
    List<T> results = new ArrayList<>();
    List<IBaseResource> allRes = BundleUtil.toListOfResources(context, bundle);
    if (allRes.size() > indexInBundle) {
      IBaseResource res = allRes.get(indexInBundle);
      if(res instanceof Bundle) {
        results.addAll(getListOfResourcesOfTypeAtIndex(context, (Bundle) res, 0, clazz));
      } else if(res.getClass().equals(clazz)){
        results.add((T)res);
      }
    }
    return results;
  }

  public <T extends IBaseResource> T getResourcesOfTypeAtIndex(FhirContext context, IBaseBundle bundle, int indexInBundle, Class<T> clazz) {
    return getListOfResourcesOfTypeAtIndex(context, bundle, indexInBundle, clazz).stream().findFirst().orElse(null);
  }
}
