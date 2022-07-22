package bio.ferlab.clin.portal.forms.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
  
  @Autowired
  private SecurityConfiguration securityConfiguration;

  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver slr = new SessionLocaleResolver();
    slr.setDefaultLocale(Locale.ENGLISH);
    return slr;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(securityConfiguration.getCors().toArray(String[]::new))
        .allowedMethods("*")
        .allowedHeaders("*")  // request allowed headers
        .exposedHeaders("*")  // response allowed headers
        .allowCredentials(true) // authorization header
        .maxAge(3600);
  }
}
