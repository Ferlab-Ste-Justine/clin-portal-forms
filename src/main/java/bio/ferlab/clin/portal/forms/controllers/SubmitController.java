package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.builders.AnalysisBuilder;
import bio.ferlab.clin.portal.forms.models.builders.ClinicalImpressionBuilder;
import bio.ferlab.clin.portal.forms.models.builders.PatientBuilder;
import bio.ferlab.clin.portal.forms.models.builders.SequencingBuilder;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/form")
@Slf4j
public class SubmitController {

  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;
  private final LocaleService localeService;
  
  public SubmitController(FhirClient fhirClient, SubmitToFhirMapper mapper, LocaleService localeService) {
    this.fhirClient = fhirClient;
    this.mapper = mapper;
    this.localeService = localeService;
  }

  @PostMapping("/{type}")
  public ResponseEntity<String> submit(@RequestHeader String authorization,
                               @PathVariable String type,
                               @Valid @RequestBody Request request) {
    /*
    1- create/get Person DONE
    2- create/get Patient DONE
    3- create servicerequest analysis
    4- create servicerequest sequencing
    5 - create specimen
    5 -create clinicalimpressoin 
    6 create obsetrvations X 
     */
    
    // the following is for SOLO only
    final PatientBuilder patientBuilder = new PatientBuilder(fhirClient, mapper, request.getPatient());
    PatientBuilder.Result pbr = patientBuilder
        .validateEp()
        .validateRamqAndMrn()
        .findByRamq()
        .findByMrn()
        .build();
    
    // TODO Observation builder

    final ClinicalImpressionBuilder clinicalImpressionBuilder = new ClinicalImpressionBuilder(mapper, pbr.getPatient());
    ClinicalImpressionBuilder.Result cbr = clinicalImpressionBuilder
        .build();
    
    final AnalysisBuilder analysisBuilder = new AnalysisBuilder(mapper, type, pbr.getPatient(), cbr.getClinicalImpression());
    AnalysisBuilder.Result abr = analysisBuilder
        .build();

    final SequencingBuilder sequencingBuilder = new SequencingBuilder(mapper, type, pbr.getPatient(), abr.getAnalysis());
    SequencingBuilder.Result sbr = sequencingBuilder
        .build();
    
    submit(pbr, abr, sbr, cbr);
    
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
  
  private void submit(PatientBuilder.Result pbr, AnalysisBuilder.Result abr, SequencingBuilder.Result sbr, ClinicalImpressionBuilder.Result cbr) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(pbr.getPatient()))
        .setResource(pbr.getPatient())
        .getRequest()
        .setUrl(FhirUtils.formatResource(pbr.getPatient())) // full url with ID required if PUT
        .setMethod(pbr.isPatientNew() ? Bundle.HTTPVerb.POST: Bundle.HTTPVerb.PUT);

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(pbr.getPerson()))
        .setResource(pbr.getPerson())
        .getRequest()
        .setUrl(FhirUtils.formatResource(pbr.getPerson())) // full url with ID required if PUT
        .setMethod(pbr.isPersonNew() ? Bundle.HTTPVerb.POST: Bundle.HTTPVerb.PUT);

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(abr.getAnalysis()))
        .setResource(abr.getAnalysis())
        .getRequest()
        .setUrl("ServiceRequest")
        .setMethod(Bundle.HTTPVerb.POST);

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(sbr.getSequencing()))
        .setResource(sbr.getSequencing())
        .getRequest()
        .setUrl("ServiceRequest")
        .setMethod(Bundle.HTTPVerb.POST);

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(cbr.getClinicalImpression()))
        .setResource(cbr.getClinicalImpression())
        .getRequest()
        .setUrl("ClinicalImpression")
        .setMethod(Bundle.HTTPVerb.POST);
    
    for(Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      log.info(entry.getRequest().getMethod() + " " + entry.getFullUrl());
    }

    fhirClient.logDebug(bundle);
    
    fhirClient.validate(bundle);
    
    Bundle response = this.fhirClient.getGenericClient().transaction().withBundle(bundle).encodedJson().execute();
    fhirClient.logDebug(response);
  }
  
}
