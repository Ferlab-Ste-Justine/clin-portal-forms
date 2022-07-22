package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {
  @Valid
  private Patient patient = new Patient();
}
