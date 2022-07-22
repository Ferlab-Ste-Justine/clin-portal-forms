package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {
  @NotNull
  private String ep;
  private String ramq;
  private String mrn;
  @NotNull
  private String firstName;
  @NotNull
  private String lastName;
  @NotNull
  private LocalDate birthDate;
  @NotNull
  private String gender;
}
