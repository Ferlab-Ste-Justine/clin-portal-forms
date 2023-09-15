package bio.ferlab.clin.portal.forms.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
    slr.setDefaultLocale(Locale.ENGLISH); // avoid exceptions to be translated into system lang
    return slr;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns(securityConfiguration.getCors().toArray(String[]::new))
        .allowedMethods(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name(),HttpMethod.POST.name())
        .allowedHeaders(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_DISPOSITION)
        .allowCredentials(true)
        .maxAge(3600);
  }
}
