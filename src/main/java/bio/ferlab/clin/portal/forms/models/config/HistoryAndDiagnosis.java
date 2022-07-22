package bio.ferlab.clin.portal.forms.models.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryAndDiagnosis {
  private List<ValueName> parentalLinks = new ArrayList<>();
  private List<ValueName> ethnicities = new ArrayList<>();
}
