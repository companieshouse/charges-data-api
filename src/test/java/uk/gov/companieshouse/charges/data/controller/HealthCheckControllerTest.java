package uk.gov.companieshouse.charges.data.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthCheckControllerTest {

    HealthCheckController controller;

    @BeforeEach
    void setup() {
        controller = new HealthCheckController();
    }

    @Test
    void testHealthCheck() {
        ResponseEntity<String> response = controller.healthcheck();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("OK", response.getBody());
    }
}