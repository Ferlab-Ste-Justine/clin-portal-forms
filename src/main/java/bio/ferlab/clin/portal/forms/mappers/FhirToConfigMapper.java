package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.config.ValueName;
import bio.ferlab.clin.portal.forms.models.config.ValueNameExtra;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FhirToConfigMapper {
  
  public Set<String> mapToAnalyseCodes( CodeSystem analyseCode) {
    return analyseCode.getConcept().stream().map(CodeSystem.ConceptDefinitionComponent::getCode).collect(Collectors.toSet());
  }
  
  public List<ValueName> mapToPrescribingInst(List<PractitionerRole> practitionerRoles) {
    return practitionerRoles.stream().map(r -> {
      String orgId = r.getOrganization().getReferenceElement().getIdPart();
      return ValueName.builder().name(orgId).value(orgId).build();
    }).collect(Collectors.toList());
  }
  
  public List<ValueName> mapToClinicalSigns(CodeSystem hp) {
    return hp.getConcept().stream()
        .skip(1)
        .limit(10)
        .map(c -> ValueName.builder().name(c.getDisplay()).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueName> mapToClinicalSigns(ValueSet hpByType) {
    return hpByType.getCompose().getIncludeFirstRep().getConcept().stream()
        .map(c -> ValueName.builder().name(c.getDisplay()).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueName> mapToOnsetAge(ValueSet age, String lang) {
    return age.getCompose().getIncludeFirstRep().getConcept().stream()
        .map(c -> ValueName.builder().name(getDisplayForLang(c, lang)).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueName> mapToParentalLinks(CodeSystem links, String lang) {
    return links.getConcept().stream()
        .map(c -> ValueName.builder().name(getDisplayForLang(c, lang)).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueName> mapToEthnicities(CodeSystem ethnicity, String lang) {
    return ethnicity.getConcept().stream()
        .map(c -> ValueName.builder().name(getDisplayForLang(c, lang)).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueNameExtra> mapToParaclinicalExams(CodeSystem observation, String lang) {
    return observation.getConcept().stream()
        .map(c -> ValueNameExtra.builder().name(getDisplayForLang(c, lang)).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueNameExtra> mapToParaclinicalExams(ValueSet observation, String lang) {
    return observation.getCompose().getIncludeFirstRep().getConcept().stream()
        .map(c -> ValueNameExtra.builder().name(getDisplayForLang(c, lang)).value(c.getCode()).build()).collect(Collectors.toList());
  }
  
  private String getDisplayForLang(ValueSet.ConceptReferenceComponent concept, String lang) {
    return concept.getDesignation().stream().filter(c -> lang.equals(c.getLanguage()))
        .map(ValueSet.ConceptReferenceDesignationComponent::getValue)
        .findFirst().orElse(concept.getDisplay());
  }

  private String getDisplayForLang(CodeSystem.ConceptDefinitionComponent concept, String lang) {
    return concept.getDesignation().stream().filter(c -> lang.equals(c.getLanguage()))
        .map(CodeSystem.ConceptDefinitionDesignationComponent::getValue)
        .findFirst().orElse(concept.getDisplay());
  }
}
