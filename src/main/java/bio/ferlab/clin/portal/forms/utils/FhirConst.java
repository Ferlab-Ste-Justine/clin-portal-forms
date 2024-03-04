package bio.ferlab.clin.portal.forms.utils;

public class FhirConst {

  private FhirConst(){}

  public static final String LDM_PREFIX = "LDM";
  public static final String DOCTOR_PREFIX = "doctor";
  public static final String RESIDENT_PREFIX = "405277009";

  public static final String PRENATAL = "Prenatal";

  public static final String TOKEN_ATTR_REALM_ACCESS = "realm_access";
  public static final String USER_ROLES_CLIN_PREFIX = "clin";
  public static final String USER_ROLE_PRESCRIBER = "clin_prescriber";
  public static final String USER_ROLE_GENETICIAN = "clin_genetician";

  public static final String DEFAULT_HPO_SUFFIX = "-default-hpo";
  public static final String DEFAULT_EXAM_SUFFIX = "-default-exam";
  public static final String ABNORMALITIES_SUFFIX = "-abnormalities";

  public static final String GESTATIONAL_AGE_EXT = "http://fhir.cqgc.ferlab.bio/StructureDefinition/gestational-age";
  public static final String SYSTEM_GESTATIONAL_AGE = "https://loinc.org";
  public static final String CODE_GESTATIONAL_AGE = "18185-9";
  public static final String SYSTEM_DDM = "https://loinc.org";
  public static final String CODE_DDM = "8665-2";
  public static final String DISPLAY_DDM = "Date last menstrual period";
  public static final String SYSTEM_DPA = "https://loinc.org";
  public static final String CODE_DPA = "11778-8";
  public static final String DISPLAY_DPA = "Delivery date Estimated";
  public static final String SYSTEM_RELATIONSHIP = "http://fhir.cqgc.ferlab.bio/CodeSystem/fmh-relationship-plus";
  public static final String SYSTEM_ROLE = "http://terminology.hl7.org/CodeSystem/v3-RoleCode";
  public static final String ROLE_CODE_MOTHER = "MTH";
  public static final String SYSTEM_RAMQ = "http://terminology.hl7.org/CodeSystem/v2-0203";
  public static final String CODE_RAMQ = "JHN";
  public static final String SYSTEM_MRN = "http://terminology.hl7.org/CodeSystem/v2-0203";
  public static final String CODE_MRN = "MR";

  public static final String ANALYSIS_SERVICE_REQUEST = "http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-analysis-request";
  public static final String SEQUENCING_SERVICE_REQUEST = "http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-sequencing-request";
  public static final String OBSERVATION_PROFILE = "http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-observation";
  public static final String PRACTITIONER_ROLE_GENETICIAN_SYSTEM = "http://snomed.info/sct";
  public static final String GENETICIAN_CODE = "15941008";

  public static final String AGE_AT_EVENT_EXT = "http://fhir.cqgc.ferlab.bio/StructureDefinition/age-at-event";
  public static final String AGE_AT_ONSET_EXT = "http://fhir.cqgc.ferlab.bio/StructureDefinition/age-at-onset";

  public static final String SUPERVISOR_EXT = "http://fhir.cqgc.ferlab.bio/StructureDefinition/resident-supervisor";

  public static final String ANALYSIS_REQUEST_CODE = "http://fhir.cqgc.ferlab.bio/CodeSystem/analysis-request-code";
  public static final String SEQUENCING_REQUEST_CODE = "http://fhir.cqgc.ferlab.bio/CodeSystem/sequencing-request-code";

  public static final String OBSERVATION_INTERPRETATION = "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation";
  public static final String OBSERVATION_CODE = "http://fhir.cqgc.ferlab.bio/CodeSystem/observation-code";
  public static final String OBSERVATION_CATEGORY = "http://terminology.hl7.org/CodeSystem/observation-category";

  public static final String HP_CODE = "http://fhir.cqgc.ferlab.bio/CodeSystem/hp";

  public static final String ETHNICITY_CODE = "http://fhir.cqgc.ferlab.bio/CodeSystem/qc-ethnicity";

  public static final String FAMILY_MEMBER = "http://fhir.cqgc.ferlab.bio/StructureDefinition/family-member";
  public static final String FAMILY_MEMBER_MOTHER_CODE = "MTH";
  public static final String FAMILY_MEMBER_FATHER_CODE = "FTH";

  public static final String SYSTEM_MISSING_PARENT = "http://fhir.cqgc.ferlab.bio/CodeSystem/missing-parent-status";
  public static final String CODE_MISSING_PARENT_TEMPORARY = "TEMPORARY";
  public static final String CODE_MISSING_PARENT_PERMANENT = "NEVER";

  public static final String MEDICAL_LICENSE_CODE = "MD";

}
