package uk.gov.companieshouse.charges.data.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest
class ExceptionHandlerConfigTest {


    @Mock
    private WebRequest request;

    private ExceptionHandlerConfig exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new ExceptionHandlerConfig(mock(Logger.class));
    }

    @Test
    @DisplayName("Handle Generic Exception")
    void handleGenericExceptionTest() {
        Exception exp = new Exception("some error");
        ResponseStatusException rse = new ResponseStatusException(404, exp.getMessage(), exp);

        ResponseEntity<Object> response = exceptionHandler.handleException(rse, request);
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Handle HttpMessageNotReadableException thrown when payload not deserialised")
    void handleHttpMessageNotReadableExceptionTest() {
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
        ResponseEntity<Object> response = exceptionHandler.handleException(exp, request);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("Handle Exception when KafkaApi returns Non 200 Response")
    void handleExceptionKafkaNon200ResponseTest() {
        ResponseStatusException rse = new ResponseStatusException(Objects.requireNonNull(HttpStatus.resolve(404)), "invokeChsKafkaApi");
        ResponseEntity<Object> response = exceptionHandler.handleException(rse, request);
        assertEquals(510, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("Handle DataAccessResourceFailureException")
    void handleCausedByIOExceptionTest() {
        DataAccessResourceFailureException exp = new DataAccessResourceFailureException("Test exception");
        ResponseEntity<Object> response = exceptionHandler.handleException(exp, request);
        assertEquals(503, response.getStatusCodeValue());
    }

}
