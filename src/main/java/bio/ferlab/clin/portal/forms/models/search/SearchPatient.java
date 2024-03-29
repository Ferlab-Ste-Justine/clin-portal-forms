package bio.ferlab.clin.portal.forms.models.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchPatient {
  private String firstName;
  private String lastName;
  private String gender;
  private String ep;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private Date birthDate;
  private String ramq;
  private String mrn;
}
