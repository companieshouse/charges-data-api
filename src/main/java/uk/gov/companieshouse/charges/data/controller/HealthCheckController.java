package uk.gov.companieshouse.charges.data.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HealthCheckController {

    @GetMapping("charges-data-api/healthcheck")
    public ResponseEntity<String> healthcheck() {
        return ResponseEntity.ok().body("OK");
    }
}