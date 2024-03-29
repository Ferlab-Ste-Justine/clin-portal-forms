package bio.ferlab.clin.portal.forms.controllers;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class ErrorController {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception e) {
    log.error("", e); // hide from the user + log the reason
    return new ResponseEntity<>("internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(FhirClientConnectionException.class)
  public ResponseEntity<String> handleException(FhirClientConnectionException e) {
    log.error("", e); // hide from the user + log the reason
    return new ResponseEntity<>("failed to connect to fhir", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> handleException(HttpMessageNotReadableException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<String> handleException(ResponseStatusException e) {
    return new ResponseEntity<>(e.getReason(), e.getStatusCode());
  }

  @ExceptionHandler(ForbiddenOperationException.class)
  public ResponseEntity<String> handleException(ForbiddenOperationException e) {
    // FHIR could complain about invalid auth
    return new ResponseEntity<>(e.getResponseBody(), HttpStatus.valueOf(e.getStatusCode()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<String> handleException(AuthenticationException e) {
    // FHIR could complain about invalid auth
    return new ResponseEntity<>(e.getResponseBody(), HttpStatus.valueOf(e.getStatusCode()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<String> handleException(MissingServletRequestParameterException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<List<String>> handleException(MethodArgumentNotValidException ex) {
    List<String> errors = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.add(error.getField() + ": " + error.getDefaultMessage());
    }
    for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
      errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
    }
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<String> handle(NoResourceFoundException e) {
    return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
  }
}
