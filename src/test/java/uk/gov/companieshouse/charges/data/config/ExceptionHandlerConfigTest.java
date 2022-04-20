package uk.gov.companieshouse.charges.data.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest
public class ExceptionHandlerConfigTest {


    @Mock
    private WebRequest request;

    private ExceptionHandlerConfig exceptionHanler;

    @BeforeEach
    void setUp() {
        exceptionHanler = new ExceptionHandlerConfig(mock(Logger.class));
    }

    @Test
    @DisplayName("Handle Generic Exception")
    public void handleGenericExceptionTest() {
        Exception exp = new Exception("some error");
        ResponseStatusException rse = new ResponseStatusException(404, exp.getMessage(), exp);

        ResponseEntity<Object> response = exceptionHanler.handleException(rse, request);
        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("Handle HttpMessageNotReadableException thown when patload not deserialised")
    public void handleHttpMessageNotReadableExceptionTest() {
        HttpInputMessage inputMessage = new HttpInputMessage() {
            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public InputStream getBody() throws IOException {
                return null;
            }
        };
        HttpMessageNotReadableException exp = new HttpMessageNotReadableException("some error", inputMessage);
        ResponseEntity<Object> response = exceptionHanler.handleException(exp, request);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("Handle Exception when KafkaApi returns Non 200 Response")
    public void handleExceptionKafkaNon200ResponseTest() {
        ResponseStatusException rse = new ResponseStatusException(Objects.requireNonNull(HttpStatus.resolve(404)), "invokeChsKafkaApi");

        ResponseEntity<Object> response = exceptionHanler.handleException(rse, request);
        assertEquals(510, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("Handle cause IOException")
    public void handleCausedByIOExceptionTest() {
        IOException ioException = new IOException("Test exception");
        ApiErrorResponseException exp = ApiErrorResponseException.fromIOException(ioException);
        ResponseStatusException rse = new ResponseStatusException(exp.getStatusCode(), exp.getStatusMessage(), exp);

        ResponseEntity<Object> response = exceptionHanler.handleException(rse, request);
        assertEquals(503, response.getStatusCodeValue());
    }

}
