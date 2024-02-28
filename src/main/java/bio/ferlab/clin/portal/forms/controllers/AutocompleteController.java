package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.autocomplete.Supervisor;
import bio.ferlab.clin.portal.forms.models.builders.AutocompleteBuilder;
import bio.ferlab.clin.portal.forms.models.builders.PractitionerBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/autocomplete")
@RequiredArgsConstructor
public class AutocompleteController {

  private final FhirClient fhirClient;

  @GetMapping("/supervisor/{ep}/{prefix}")
  public List<Supervisor> autocomplete(@RequestHeader String authorization, @PathVariable String ep, @PathVariable String prefix) {
    PractitionerBuilder.validateAccessToEp(fhirClient, authorization, ep);
    return new AutocompleteBuilder(fhirClient, ep)
        .withSupervisor(prefix)
        .build().getSupervisors();
  }

}
