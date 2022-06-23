package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import bio.ferlab.clin.portal.forms.exceptions.SecurityException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class SecurityService {

  private final SecurityConfiguration configuration;
  
  public SecurityService(SecurityConfiguration configuration) {
    this.configuration = configuration;
  }

  public void checkAuthorization(String authorization) {
    if(configuration.isEnabled()) {
      if (!StringUtils.hasText(authorization)) {
        throw new SecurityException(HttpStatus.UNAUTHORIZED, "missing token");
      }
      try {
        final String token = authorization.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(token);
        if (jwt.getExpiresAt().toInstant().isBefore(Instant.now().minusSeconds(5))) {
          throw new SecurityException(HttpStatus.FORBIDDEN, "token expired");
        }
        if (!jwt.getAudience().get(0).equals(configuration.getAudience())) {
          throw new SecurityException(HttpStatus.FORBIDDEN, "invalid audience");
        }
        if (!jwt.getIssuer().equals(configuration.getIssuer())){
          throw new SecurityException(HttpStatus.FORBIDDEN, "invalid issuer");
        }
      } catch(JWTDecodeException e) {
        throw new SecurityException(HttpStatus.FORBIDDEN, "invalid token");
      }
    }
  }
}
