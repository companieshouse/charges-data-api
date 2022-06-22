package uk.gov.companieshouse.charges.data.config;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.MethodNotAllowedException;
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
        responseBody.put("message", String.format("Exception occurred while processing the API"
                + " request with Correlation ID: %s", correlationId));
    }

    private void errorLogException(Exception ex, String correlationId) {
        logger.errorContext(null, String.format("Exception occurred while processing the "
                + "API request with Correlation ID: %s", correlationId), ex, null);
    }

    private Map<String, Object> responseAndLogBuilderHandler(Exception ex) {
        var correlationId = generateShortCorrelationId();
        Map<String, Object> responseBody = new LinkedHashMap<>();
        populateResponseBody(responseBody, correlationId);
        errorLogException(ex, correlationId);

        return responseBody;
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
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex),
                HttpStatus.INTERNAL_SERVER_ERROR);
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
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex),
                HttpStatus.SERVICE_UNAVAILABLE);
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

        if ("invokeChsKafkaApi".equals(ex.getReason())) {
            return new ResponseEntity<>(responseAndLogBuilderHandler(ex),
                    HttpStatus.NOT_EXTENDED);
        }

        if (HttpStatus.SERVICE_UNAVAILABLE.equals(ex.getStatus())) {
            return new ResponseEntity<>(responseAndLogBuilderHandler(ex),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex),
                HttpStatus.INTERNAL_SERVER_ERROR);
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
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex),
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Runtime exception handler MethodArgumentNotValidException.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<Object> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex),
                HttpStatus.BAD_REQUEST);
    }

    /**
     * MethodNotAllowedException exception handler.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {MethodNotAllowedException.class,
            HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<Object> handleMethodNotAllowedException(Exception ex,
                                                                  WebRequest request) {
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex),
                HttpStatus.METHOD_NOT_ALLOWED);

    }

    private String generateShortCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
