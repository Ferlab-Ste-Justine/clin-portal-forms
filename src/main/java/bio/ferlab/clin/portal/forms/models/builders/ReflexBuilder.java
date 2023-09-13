package bio.ferlab.clin.portal.forms.models.builders;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReflexBuilder {

  public static final String REFLEX_PANEL_PREFIX_FR = "Panel réflexe: ";
  public static final String REFLEX_PANEL_PREFIX_EN = "Reflex Panel: ";

  /*private final CodesValuesService codesValuesService;
  private final FhirToConfigMapper fhirToConfigMapper;*/
  private final String lang;
  private final Boolean isReflex;

  public Result build() {
    String reflex = null;
    if (isReflex) {
      if ("fr".equals(lang)) {
        reflex = String.format(REFLEX_PANEL_PREFIX_FR + "%s (%s)", "Maladies musculaires globales", "MMG");
      } else {
        reflex = String.format(REFLEX_PANEL_PREFIX_EN + "%s (%s)", "Global Muscular diseases", "MMG");
      }
      /*final CodeSystem analyse = codesValuesService.getCodes(CodesValuesService.ANALYSE_KEY);
      reflex = analyse.getConcept().stream()
        .filter(c -> "MMG".equals(c.getCode()))
        .findFirst()
        .map(c -> fhirToConfigMapper.getDisplayForLang(c, lang))
        .orElse(String.format("Reflex Panel: %s (%s)", "Global Muscular diseases", "MMG"));*/
      // Reflex Panel: Global Muscular diseases (MMG)
    }
    return new Result(reflex);
  }

  @Getter
  @AllArgsConstructor
  public static class Result {
    private String reflex;
  }

}
