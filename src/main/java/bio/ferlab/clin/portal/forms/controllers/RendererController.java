package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.TemplateMapper;
import bio.ferlab.clin.portal.forms.services.*;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.DateUtils;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@Controller
@RequestMapping("/render")
@RequiredArgsConstructor
public class RendererController {

  enum Format {
    html, pdf;
    boolean is(String format) {
      return this.name().equalsIgnoreCase(format);
    }
  }

  private final LocaleService localeService;
  private final LogOnceService logOnceService;
  private final FhirClient fhirClient;
  private final TemplateService templateService;
  private final CodesValuesService codesValuesService;
  private final MessagesService messagesService;

  @GetMapping(path = "/{id}")
  public ResponseEntity<?> render(@PathVariable String id, @RequestParam(defaultValue = "html") String format) {

    final var locale = LocaleService.DEFAULT_LOCALE; // localeService.getCurrentLocale();
    final var context =  prepareContext(id, locale);
    final var template = templateService.parseTemplate("index", context, locale);

    if (Format.html.is(format)) {
      return renderAsHtml(template);
    } else if (Format.pdf.is(format)) {
      return renderAsPdf(id, template);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: " + format);
    }
  }

  private Map<String, Object> prepareContext(String id, Locale locale) {
    final var mainBundle = fhirClient.findServiceRequestWithDepsById(id);
    final var mainBundleExtractor = new BundleExtractor(fhirClient.getContext(), mainBundle);
    final var analysis = mainBundleExtractor.getFirstResourcesOfType(ServiceRequest.class);
    // if the user doesn't belong to the EP/LDM
    if (analysis == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found: " + id);
    // has to be analysis
    if (!analysis.getMeta().hasProfile(ANALYSIS_SERVICE_REQUEST)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prescription isn't an analysis: " + id);
    }
    // foetus not supported
    if (analysis.hasCategory() && PRENATAL.equalsIgnoreCase(analysis.getCategoryFirstRep().getText())) {
      throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Prescription for foetus isn't implemented: " + id);
    }
    final var performer = mainBundleExtractor.getFirstResourcesOfType(Organization.class);
    // Assignation feature will attach several PractitionerRole insider performer BUT we want the one from requester
    final var practitionerRole = mainBundleExtractor.getFirstResourcesById(PractitionerRole.class, FhirUtils.extractId(analysis.getRequester()));

    // DUO/TRIO ...
    var familyMembers = new TreeMap<String, Reference>();
    for(var member: analysis.getExtensionsByUrl(FAMILY_MEMBER)) {
      var parentPatientRef = ((Reference) member.getExtensionByUrl("parent").getValue());
      var parentRelation = ((CodeableConcept) member.getExtensionByUrl("parent-relationship").getValue()).getCodingFirstRep().getCode();
      familyMembers.put(parentRelation, parentPatientRef);
    }

    final var detailsBundle = fhirClient.fetchPrescriptionDetails(analysis, practitionerRole, familyMembers);
    final var detailsBundleExtractor = new BundleExtractor(fhirClient.getContext(), detailsBundle);
    final var sequencings = detailsBundleExtractor.getAllResourcesOfType(ServiceRequest.class).stream()
      .filter(s -> s.getMeta().hasProfile(SEQUENCING_SERVICE_REQUEST)).toList();
    final var patients = detailsBundleExtractor.getAllResourcesOfType(Patient.class);
    final var persons = detailsBundleExtractor.getAllResourcesOfType(Person.class);
    final var practitioner = detailsBundleExtractor.getFirstResourcesOfType(Practitioner.class);
    final var organization = detailsBundleExtractor.getFirstResourcesOfType(Organization.class);
    final var impressions = detailsBundleExtractor.getAllResourcesOfType(ClinicalImpression.class);
    final var observations = detailsBundleExtractor.getAllResourcesOfType(Observation.class);
    final var familyHistories = detailsBundleExtractor.getAllResourcesOfType(FamilyMemberHistory.class);

    final var analysisCodes = codesValuesService.getCodes(CodesValuesService.ANALYSE_KEY);

    final var probandPatient = patients.stream()
      .filter(s -> s.getIdElement().getIdPart().equals(FhirUtils.extractId(analysis.getSubject())))
      .findFirst().orElseThrow(() -> new RuntimeException("Can't find patient for analysis: " + analysis.getIdElement().getIdPart() + " and subject: " + analysis.getSubject().getReference()));
    final var probandPerson = persons.stream()
      .filter(s -> s.getLink().stream().anyMatch(l -> l.getTarget().getReference().equals(analysis.getSubject().getReference())))
      .findFirst().orElseThrow(() -> new RuntimeException("Can't find person for analysis: " + analysis.getIdElement().getIdPart() + " and subject: " + analysis.getSubject().getReference()));
    final var probandSequencing = sequencings.stream()
      .filter(s -> analysis.getSubject().getReference().equals(s.getSubject().getReference()))
      .findFirst().orElseThrow(() -> new RuntimeException("Can't find sequencing for analysis: " + analysis.getIdElement().getIdPart() + " and subject: " + analysis.getSubject().getReference()));
    final var probandImpression = impressions.stream()
      .filter(s -> analysis.getSubject().getReference().equals(s.getSubject().getReference()))
      .findFirst().orElseThrow(() -> new RuntimeException("Can't find clinical impression for analysis: " + analysis.getIdElement().getIdPart() + " and subject: " + analysis.getSubject().getReference()));
    final var probandObservations = observations.stream()
      .filter(s -> analysis.getSubject().getReference().equals(s.getSubject().getReference()))
      .toList();
    final var probandFamilyHistories = familyHistories.stream()
      .filter(s -> analysis.getSubject().getReference().equals(s.getPatient().getReference()))
      .toList();

    // extract family infos
    var probandFamily = new ArrayList<FamilyMember>();
    for(var relation : familyMembers.keySet()) {
      var ref = familyMembers.get(relation);
      final var patient = patients.stream()
        .filter(s -> s.getIdElement().getIdPart().equals(FhirUtils.extractId(ref)))
        .findFirst().orElseThrow(() -> new RuntimeException("Can't find patient ("+relation+") for analysis: " + analysis.getIdElement().getIdPart() + " and parent: " + ref.getReference()));
      final var person = persons.stream()
        .filter(s -> s.getLink().stream().anyMatch(l -> l.getTarget().getReference().equals(ref.getReference())))
        .findFirst().orElseThrow(() -> new RuntimeException("Can't find person ("+relation+") for analysis: " + analysis.getIdElement().getIdPart() + " and parent: " + ref.getReference()));
      var sequencing = sequencings.stream()
        .filter(s -> ref.getReference().equals(s.getSubject().getReference()))
        .findFirst().orElseThrow(() -> new RuntimeException("Can't find sequencing ("+relation+") for analysis: " + analysis.getIdElement().getIdPart() + " and parent: " + ref.getReference()));
      final var impression = impressions.stream()
        .filter(s -> ref.getReference().equals(s.getSubject().getReference()))
        .findFirst().orElseThrow(() -> new RuntimeException("Can't find clinical impression ("+relation+") for analysis: " + analysis.getIdElement().getIdPart() + " and parent: " + ref.getReference()));
      final var obs = observations.stream()
        .filter(s -> ref.getReference().equals(s.getSubject().getReference()))
        .toList();
      final var histories = familyHistories.stream()
        .filter(s -> ref.getReference().equals(s.getPatient().getReference()))
        .toList();
      probandFamily.add(new FamilyMember(null, ref, relation, patient, person, sequencing, impression, obs, histories, null));
    }

    var missingReasons = probandObservations.stream()
      .filter(o -> o.getCategoryFirstRep().getCodingFirstRep().getCode().equals("social-history"))
      .filter(o -> o.getValueCodeableConcept().getCodingFirstRep().getSystem().equals(SYSTEM_MISSING_PARENT))
      .toList();
    for(var missingReason : missingReasons) {
      var reason = missingReason.getNoteFirstRep().getText();
      var relation = StringUtils.substring(missingReason.getCode().getCodingFirstRep().getCode(), 1);
      if (StringUtils.isNoneBlank(reason, relation)) {
        probandFamily.add(new FamilyMember(reason, null, relation, null, null, null, null, null, null, missingReason.getValueCodeableConcept().getCodingFirstRep().getCode()));
      }
    }

    final Map<String, Object> context = new HashMap<>();

    context.put("analysis", analysis);

    context.put("performer", performer);
    context.put("practitionerRole", practitionerRole);
    context.put("practitioner", practitioner);
    context.put("organization", organization);

    context.put("probandSequencing", probandSequencing);
    context.put("probandPatient", probandPatient);
    context.put("probandPerson", probandPerson);
    context.put("probandImpression", probandImpression);
    context.put("probandObservations", probandObservations);
    context.put("probandFamilyHistories", probandFamilyHistories);

    context.put("probandFamilyMembers", probandFamily);

    // don't know how thread-safe is Pebble renderer, let's instance a new mapper instead of having a singleton
    context.put("mapper", new TemplateMapper(id, logOnceService, messagesService, templateService, codesValuesService, analysisCodes, locale));
    context.put("now", new Date());
    context.put("version", "1.0");

    FhirUtils.findExtension(analysis, SUPERVISOR_EXT).ifPresent(r -> {
      final Bundle bundle = fhirClient.findPractitionerAndRoleByRoleId(FhirUtils.extractId((Reference)r));
      final var extractor = new BundleExtractor(fhirClient.getContext(), bundle);
      context.put("supervisor", extractor.getFirstResourcesOfType(Practitioner.class));
      context.put("supervisorRole", extractor.getFirstResourcesOfType(PractitionerRole.class));
    });


    //context.put("analysisBarcodeBase64", templateService.convertToBase64(templateService.generateBarcodeImage(analysis.getIdElement().getIdPart())));
    //context.put("sequencingBarcodeBase64", templateService.convertToBase64(templateService.generateBarcodeImage(sequencing.getIdElement().getIdPart())));

    return context;
  }

  private ResponseEntity<String> renderAsHtml(String template) {
    return ResponseEntity.ok()
      .contentType(MediaType.TEXT_HTML)
      .body(template);
  }

  private ResponseEntity<ByteArrayResource> renderAsPdf(String id, String template) {
    byte[] pdf = templateService.convert(template);
    ByteArrayResource resource = new ByteArrayResource(pdf);
    final String fileName = String.format("%s_%s.pdf", id, LocalDateTime.now().format(DateUtils.FORMATTER_YYYYMMddTHHmmss));
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
      .contentLength(resource.contentLength())
      .contentType(MediaType.APPLICATION_PDF)
      .body(resource);
  }

  public record FamilyMember(String missingReason, Reference ref, String relation, Patient patient, Person person, ServiceRequest sequencing, ClinicalImpression clinicalImpression, List<Observation> observations, List<FamilyMemberHistory> familyHistories, String missingReasonType){}
}
