package uk.gov.companieshouse.charges.data.config;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import uk.gov.companieshouse.logging.Logger;

@ControllerAdvice
public class ExceptionHandlerConfig {

    private final Logger logger;

    public ExceptionHandlerConfig(final Logger logger) {
        this.logger = logger;
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
        logger.error(String.format("Started: handleException: %s Generating error response for "
                        + "Exception: % "
                        + "Cause: %"
                        + "StackTrace: % ",
                correlationId, ex.getClass().toString(), ex.getCause().toString(),
                Arrays.toString(ex.getStackTrace())));
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("message", "There is issue completing the request.");
        responseBody.put("correlationId", correlationId);
        request.setAttribute("javax.servlet.error.exception", ex, 0);
        logger.error(String.format("Finished: handleException: %s handleException", correlationId));
        return new ResponseEntity(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String generateShortCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
