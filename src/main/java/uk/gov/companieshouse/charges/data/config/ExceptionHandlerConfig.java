package uk.gov.companieshouse.charges.data.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.logging.Logger;

@ControllerAdvice
public class ExceptionHandlerConfig {

    private final Logger logger;

    public ExceptionHandlerConfig(final Logger logger) {
        this.logger = logger;
    }

    private void populateResponseBody(Map<String, Object> responseBody , String correlationId) {
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("message", "There is issue completing the request.");
        responseBody.put("correlationId", correlationId);
    }

    /**
     * Runtime exception handler.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<Object> handleException(Exception ex, WebRequest request) {
        var correlationId = generateShortCorrelationId();
        logger.error(String.format("Started: handleException: %s Generating error response ",
                correlationId), ex);
        Map<String, Object> responseBody = new LinkedHashMap<>();
        populateResponseBody(responseBody, correlationId);
        request.setAttribute("javax.servlet.error.exception", ex, 0);
        logger.error(String.format("Finished: handleException: %s handleException", correlationId));
        return new ResponseEntity(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Runtime exception handler DataAccessResourceFailureException.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {DataAccessResourceFailureException.class})
    public ResponseEntity<Object> handleException(DataAccessResourceFailureException ex,
            WebRequest request) {
        var correlationId = generateShortCorrelationId();
        logger.error(String.format("Started: handleException: %s Generating error response ",
                correlationId), ex);
        Map<String, Object> responseBody = new LinkedHashMap<>();
        populateResponseBody(responseBody, correlationId);
        Throwable cause = ex.getCause();

        return new ResponseEntity(responseBody, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
    * Runtime exception handler ResponseStatusException.
    *
    * @param ex      exception to handle.
    * @param request request.
    * @return error response to return.
    */
    @ExceptionHandler(value = { ResponseStatusException.class })
    public ResponseEntity<Object> handleException(ResponseStatusException ex, WebRequest request) {
        var correlationId = generateShortCorrelationId();
        logger.error(String.format("Started: handleException: %s Generating error response ",
                correlationId), ex);
        Map<String, Object> responseBody = new LinkedHashMap<>();
        populateResponseBody(responseBody, correlationId);

        if ("invokeChsKafkaApi".equals(ex.getReason())) {
            return new ResponseEntity(responseBody, HttpStatus.NOT_EXTENDED);
        }

        if (HttpStatus.SERVICE_UNAVAILABLE.equals(ex.getStatus())) {
            return new ResponseEntity(responseBody, HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new ResponseEntity(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * Runtime exception handler HttpMessageNotReadableException.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {HttpMessageNotReadableException.class})
    public ResponseEntity<Object> handleException(HttpMessageNotReadableException ex,
            WebRequest request) {
        var correlationId = generateShortCorrelationId();
        logger.error(String.format("Started: handleException: %s Generating error response ",
                correlationId), ex);
        Map<String, Object> responseBody = new LinkedHashMap<>();
        populateResponseBody(responseBody, correlationId);
        return new ResponseEntity(responseBody, HttpStatus.BAD_REQUEST);
    }

    private String generateShortCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
