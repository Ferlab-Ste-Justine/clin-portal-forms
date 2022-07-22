package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.configurations.CacheConfiguration;
import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import bio.ferlab.clin.portal.forms.mappers.FhirToConfigMapper;
import bio.ferlab.clin.portal.forms.models.config.Form;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/form")
@Slf4j
public class ConfigController {
  
  private static final String DEFAULT_HPO = "-default-hpo";
  private static final String DEFAULT_EXAM = "-default-exam";

  private static final String CACHE_INIT_KEY = "init";
  private static final String CACHE_ANALYSE_KEY = "analyse";
  private static final String CACHE_HP_KEY = "hp";
  private static final String CACHE_PARENTAL_KEY = "parental";
  private static final String CACHE_ETHNICITY_KEY = "ethnicity";
  private static final String CACHE_OBSERVATION_KEY = "observation";
  private static final String CACHE_AGE_KEY = "age";
  private static final String CACHE_HP_BY_TYPE_KEY = "-hp";
  private static final String CACHE_OBS_BY_TYPE = "-observation";
  
  private final FhirConfiguration fhirConfiguration;
  private final FhirClient fhirClient;
  private final LocaleService localeService;
  private final FhirToConfigMapper fhirToConfigMapper;
  private final Cache cache;
  
  public ConfigController(FhirConfiguration fhirConfiguration,
                          FhirClient fhirClient,
                          LocaleService localeService,
                          FhirToConfigMapper fhirToConfigMapper,
                          CacheManager cacheManager) {
    this.fhirConfiguration = fhirConfiguration;
    this.fhirClient = fhirClient;
    this.localeService = localeService;
    this.fhirToConfigMapper = fhirToConfigMapper;
    this.cache = cacheManager.getCache(CacheConfiguration.CACHE_NAME);
  }

  @GetMapping("/{type}")
  public Form config(@RequestHeader String authorization,
                     @PathVariable String type) {
    
    final String lang = localeService.getCurrentLocale();
    final String practitionerId = JwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID);

    // codes and values are fetched once
    Map<String, IBaseResource> codesAndValues = fetchCodesAndValues();
    
    // fetch data from FHIR
    Bundle response = this.fhirClient.getGenericClient().search().forResource(PractitionerRole.class)
        .where(PractitionerRole.PRACTITIONER.hasId(practitionerId)).returnBundle(Bundle.class).encodedJson().execute();

    BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), response);
    List<PractitionerRole> practitionerRoles = bundleExtractor.getNextListOfResourcesOfType(PractitionerRole.class);
    
    CodeSystem analyseCode = (CodeSystem) codesAndValues.get(CACHE_ANALYSE_KEY);
    CodeSystem hp = (CodeSystem) codesAndValues.get(CACHE_HP_KEY);
    CodeSystem parentalLinks = (CodeSystem) codesAndValues.get(CACHE_PARENTAL_KEY);
    CodeSystem ethnicity = (CodeSystem) codesAndValues.get(CACHE_ETHNICITY_KEY);
    CodeSystem observation = (CodeSystem) codesAndValues.get(CACHE_OBSERVATION_KEY);
    ValueSet age = (ValueSet) codesAndValues.get(CACHE_AGE_KEY);
    
    // validate the form's type is supported
    if (analyseCode.getConcept().stream().noneMatch(c -> type.equals(c.getCode()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Unsupported form type: '%s' available types: %s",
          type, fhirToConfigMapper.mapToAnalyseCodes(analyseCode)));
    }
    
    // return form's config
    Form form = new Form();
    form.getConfig().getPrescribingInstitutions().addAll(fhirToConfigMapper.mapToPrescribingInst(practitionerRoles));
    form.getConfig().getClinicalSigns().getOnsetAge().addAll(fhirToConfigMapper.mapToOnsetAge(age, lang));
    form.getConfig().getHistoryAndDiagnosis().getParentalLinks().addAll(fhirToConfigMapper.mapToParentalLinks(parentalLinks, lang));
    form.getConfig().getHistoryAndDiagnosis().getEthnicities().addAll(fhirToConfigMapper.mapToEthnicities(ethnicity, lang));
    
    // use form default or generic values
    final String formType = type.toLowerCase();
    final List<ValueSet> hpByTypes = fhirConfiguration.getTypesWithDefault().stream()
        .map(t -> (ValueSet) codesAndValues.get(t + CACHE_HP_BY_TYPE_KEY)).collect(Collectors.toList());
    final List<ValueSet> obsByTypes = fhirConfiguration.getTypesWithDefault().stream()
        .map(t -> (ValueSet) codesAndValues.get(t + CACHE_OBS_BY_TYPE)).collect(Collectors.toList());
    this.applyFormHpByTypeOrDefault(formType, form, hp, hpByTypes);
    this.applyFormObservationByTypeOrDefault(formType, form, lang, observation, obsByTypes);
    
    return form;
  }
  
  private void applyFormHpByTypeOrDefault(String formType, Form form, CodeSystem all, List<ValueSet> byTypes) {
    Optional<ValueSet> byType = byTypes.stream().filter(vs -> (formType + DEFAULT_HPO).equals(vs.getName())).findFirst();
    if (byType.isPresent()) {
      form.getConfig().getClinicalSigns().getDefaultList().addAll(fhirToConfigMapper.mapToClinicalSigns(byType.get()));
    } else {
      form.getConfig().getClinicalSigns().getDefaultList().addAll(fhirToConfigMapper.mapToClinicalSigns(all));
    }
  }

  private void applyFormObservationByTypeOrDefault(String formType, Form form, String lang, CodeSystem all, List<ValueSet> byTypes) {
    Optional<ValueSet> byType = byTypes.stream().filter(vs -> (formType + DEFAULT_EXAM).equals(vs.getName())).findFirst();
    if (byType.isPresent()) {
      form.getConfig().getParaclinicalExams().getDefaultList().addAll(fhirToConfigMapper.mapToParaclinicalExams(byType.get(), lang));
    } else {
      form.getConfig().getParaclinicalExams().getDefaultList().addAll(fhirToConfigMapper.mapToParaclinicalExams(all, lang));
    }
  }
  
  private synchronized Map<String, IBaseResource> fetchCodesAndValues() {
    
    final Boolean isCacheInit = cache.get(CACHE_INIT_KEY, Boolean.class);
    
    if (isCacheInit == null || Boolean.FALSE.equals(isCacheInit)) {
      
      log.info("Fetch codes and values from FHIR");

      Bundle bundle = new Bundle();
      bundle.setType(Bundle.BundleType.BATCH);

      bundle.addEntry().getRequest()
          .setUrl("CodeSystem/analysis-request-code")
          .setMethod(Bundle.HTTPVerb.GET);

      bundle.addEntry().getRequest()
          .setUrl("CodeSystem/hp")
          .setMethod(Bundle.HTTPVerb.GET);

      bundle.addEntry().getRequest()
          .setUrl("CodeSystem/fmh-relationship-plus")
          .setMethod(Bundle.HTTPVerb.GET);

      bundle.addEntry().getRequest()
          .setUrl("CodeSystem/qc-ethnicity")
          .setMethod(Bundle.HTTPVerb.GET);

      bundle.addEntry().getRequest()
          .setUrl("CodeSystem/observation-code")
          .setMethod(Bundle.HTTPVerb.GET);

      bundle.addEntry().getRequest()
          .setUrl("ValueSet/age-at-onset")
          .setMethod(Bundle.HTTPVerb.GET);
      
      for(String byType: fhirConfiguration.getTypesWithDefault()) {
        bundle.addEntry().getRequest()
            .setUrl("ValueSet/" + byType + DEFAULT_HPO)
            .setMethod(Bundle.HTTPVerb.GET);
        bundle.addEntry().getRequest()
            .setUrl("ValueSet/" + byType + DEFAULT_EXAM)
            .setMethod(Bundle.HTTPVerb.GET);
      }
      
      Bundle response = fhirClient.getGenericClient().transaction().withBundle(bundle).encodedJson().execute();
      BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), response);

      CodeSystem analyseCode = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
      CodeSystem hp = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
      CodeSystem parentalLinks = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
      CodeSystem ethnicity = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
      CodeSystem observation = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
      ValueSet age = bundleExtractor.getNextResourcesOfType(ValueSet.class);
      
      cache.put(CACHE_INIT_KEY, Boolean.TRUE);
      cache.putIfAbsent(CACHE_ANALYSE_KEY, analyseCode);
      cache.putIfAbsent(CACHE_HP_KEY, hp);
      cache.putIfAbsent(CACHE_PARENTAL_KEY, parentalLinks);
      cache.putIfAbsent(CACHE_ETHNICITY_KEY, ethnicity);
      cache.putIfAbsent(CACHE_OBSERVATION_KEY, observation);
      cache.putIfAbsent(CACHE_AGE_KEY, age);
      
      for(String byType: fhirConfiguration.getTypesWithDefault()) {
        ValueSet hpByType = bundleExtractor.getNextResourcesOfType(ValueSet.class);
        ValueSet obsByType = bundleExtractor.getNextResourcesOfType(ValueSet.class);
        cache.putIfAbsent(byType + CACHE_HP_BY_TYPE_KEY, hpByType);
        cache.putIfAbsent(byType + CACHE_OBS_BY_TYPE, obsByType);
      }
    }

    // don't try to get the cache values out of synchronized because they could be evicted
    Map<String, IBaseResource> codesAndValues = new HashMap<>();
    codesAndValues.put(CACHE_ANALYSE_KEY, cache.get(CACHE_ANALYSE_KEY, CodeSystem.class));
    codesAndValues.put(CACHE_HP_KEY, cache.get(CACHE_HP_KEY, CodeSystem.class));
    codesAndValues.put(CACHE_PARENTAL_KEY, cache.get(CACHE_PARENTAL_KEY, CodeSystem.class));
    codesAndValues.put(CACHE_ETHNICITY_KEY, cache.get(CACHE_ETHNICITY_KEY, CodeSystem.class));
    codesAndValues.put(CACHE_OBSERVATION_KEY, cache.get(CACHE_OBSERVATION_KEY, CodeSystem.class));
    codesAndValues.put(CACHE_AGE_KEY, cache.get(CACHE_AGE_KEY, ValueSet.class));
    
    for(String byType: fhirConfiguration.getTypesWithDefault()) {
      codesAndValues.put(byType + CACHE_HP_BY_TYPE_KEY, cache.get(byType + CACHE_HP_BY_TYPE_KEY, ValueSet.class));
      codesAndValues.put(byType + CACHE_OBS_BY_TYPE, cache.get(byType + CACHE_OBS_BY_TYPE, ValueSet.class));
    }
    
    return codesAndValues;
  }
  
  public synchronized void clearCache() {
    log.info("Evict codes and values entries from cache");
    cache.put(CACHE_INIT_KEY, Boolean.FALSE);
    cache.evictIfPresent(CACHE_ANALYSE_KEY);
    cache.evictIfPresent(CACHE_HP_KEY);
    cache.evictIfPresent(CACHE_PARENTAL_KEY);
    cache.evictIfPresent(CACHE_ETHNICITY_KEY);
    cache.evictIfPresent(CACHE_OBSERVATION_KEY);
    cache.evictIfPresent(CACHE_AGE_KEY);
    
    for(String byType: fhirConfiguration.getTypesWithDefault()) {
      cache.evictIfPresent(byType + CACHE_HP_BY_TYPE_KEY);
      cache.evictIfPresent(byType + CACHE_OBS_BY_TYPE);
    }
  }

}
