package uk.gov.companieshouse.charges.data.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.charges.data.service.ChargesService;

@RestController
public class ChargesController {
    private ChargesService chargesService;

    @Autowired
    public ChargesController(ChargesService chargesService) {
        this.chargesService = chargesService;
    }

    /**
     * Get charges for company.
     *
     */
    @GetMapping(value = "/company/{company_number}/charges")
    public ResponseEntity<?> getChargesByCompanyNumber(
            @PathVariable("company_number") String companyNumber,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Get charge for company.
     *
     */
    @GetMapping(value = "/company/{company_number}/charge/{charge_id}")
    public ResponseEntity<?> getChargeByCompanyNumber(
            @PathVariable("company_number") String companyNumber,
            @PathVariable("company_number") String chargeId,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
