package uk.gov.companieshouse.charges.data.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.service.ChargesService;
import uk.gov.companieshouse.logging.Logger;

@RestController
public class ChargesController {

    private final Logger logger;
    ChargesService chargesService;

    @Autowired
    public ChargesController(final ChargesService chargesService, final Logger logger) {
        this.chargesService = chargesService;
        this.logger = logger;
    }

    /**
     * PUT request for charges.
     *
     * @param companyNumber the company number for charge
     * @param chargeId      the id for charge
     * @param requestBody   the request body containing charges data
     * @return no response
     */
    @PutMapping("/company/{company_number}/charge/{charge_id}/internal")
    public ResponseEntity<Void> saveOrUpdateCharges(
            @RequestHeader("x-request-id") final String contextId,
            @PathVariable("company_number") final String companyNumber,
            @PathVariable("charge_id") final String chargeId,
            @RequestBody final InternalChargeApi requestBody
    ) {
        logger.debug(String.format(
                "Started : Save or Update charge %s with company number %s ",
                chargeId,
                companyNumber));
        this.chargesService.upsertCharges(contextId, companyNumber, chargeId, requestBody);
        logger.debug(String.format(
                "Finished : Save or Update charge %s with company number %s ",
                chargeId,
                companyNumber));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Retrieve a company charge details using a company number and chargeId.
     *
     * @param companyNumber the company number of the company
     * @param chargeId      the chargeId
     * @return company charge api
     */
    @GetMapping("/company/{company_number}/charges/{charge_id}")
    public ResponseEntity<ChargeApi> getCompanyCharge(
            @PathVariable("company_number") final String companyNumber,
            @PathVariable("charge_id") final String chargeId) {
        logger.debug(String.format("Started : get Charge Details for Company Number %s "
                        + " Charge Id %s ",
                companyNumber,
                chargeId
        ));
        ResponseEntity<ChargeApi> chargeApiResponse =
                chargesService.getChargeDetails(companyNumber, chargeId).map(chargesDocument ->
                                new ResponseEntity<>(
                                        chargesDocument,
                                        HttpStatus.OK))
                        .orElse(ResponseEntity.notFound().build());
        logger.debug(String.format("Finished : %s Charge details found for Company Number %s "
                        + "with Charge id %s",
                chargeApiResponse.getStatusCode() != HttpStatus.OK ? "No" : "",
                companyNumber,
                chargeId
        ));
        return chargeApiResponse;
    }

    /**
     * Retrieve a company charges using a company number.
     *
     * @param companyNumber the company number of the company
     * @return company charge api
     */
    @GetMapping("/company/{company_number}/charges/{items_per_page}/{start_index}")
    public ResponseEntity<ChargesApi> getCompanyCharges(
            @PathVariable("company_number") final String companyNumber,
            @PathVariable("items_per_page") final Integer itemsPerPage,
            @PathVariable("start_index") final Integer startIndex) {
        logger.debug(String.format("Started : getCompanyCharges Charges for Company Number %s ",
                companyNumber
        ));
        Pageable pageable = PageRequest.of(startIndex, itemsPerPage);
        ResponseEntity<ChargesApi> chargesApiResponseEntity =
                chargesService.findCharges(companyNumber, pageable).map(chargesDocument ->
                                new ResponseEntity<>(
                                        chargesDocument,
                                        HttpStatus.OK))
                        .orElse(ResponseEntity.notFound().build());

        logger.debug(
                String.format("Finished : getCompanyCharges Charges found for Company Number %s ",
                        companyNumber
                ));
        return chargesApiResponseEntity;
    }

}
