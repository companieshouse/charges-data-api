package uk.gov.companieshouse.charges.data.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.model.charges.ChargesApi;
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
            @PathVariable("charge_id") String chargeId,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Add or update individual charge.
     *
     * @param  companyNumber  the company number for charge
     * @param chargeId the Id for charge
     * @param  requestBody  the request body containing insolvency data
     * @return  ResponseEntity
     */
    @PutMapping("/company/{company_number}/charge/{charge_id}")
    public ResponseEntity<Void> addUpdateCharge(
            @PathVariable("company_number") int companyNumber,
            @PathVariable("charge_id") String chargeId,
            @RequestBody ChargesApi requestBody
    ) {
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
