package bio.ferlab.clin.portal.forms.models.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValueNameExtra {
  private String name;
  private String value;
  private Extra extra;
}
