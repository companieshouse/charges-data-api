package uk.gov.companieshouse.charges.data.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.service.ChargesService;

@RestController
public class ChargesController {

    ChargesService chargesService;

    @Autowired
    public ChargesController(ChargesService chargesService) {
        this.chargesService = chargesService;
    }

    /**
     * PUT request for charges.
     *
     * @param  companyNumber  the company number for charge
     * @param  chargeId  the id for charge
     * @param  requestBody  the request body containing charges data
     * @return  no response
     */
    @PutMapping("/company/{company_number}/charge/{charge_id}/internal")
    public ResponseEntity<Void> saveOrUpdateCharges(
            @PathVariable("company_number") final String companyNumber,
            @PathVariable("charge_id") final String chargeId,
            @RequestBody final InternalChargeApi requestBody
    ) {
        this.chargesService.upsertCharges(companyNumber, chargeId, requestBody);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
