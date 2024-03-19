package uk.gov.companieshouse.charges.data.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.model.RequestCriteria;
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
            @Valid @RequestBody final InternalChargeApi requestBody
    ) {
        logger.debug(String.format(
                "Payload Successfully received on PUT with contextId %s and company number %s",
                contextId,
                companyNumber));

        logger.debug(String.format(
                "Started : Save or Update charge %s with company number %s ",
                chargeId,
                companyNumber));
        chargesService.upsertCharges(contextId, companyNumber, chargeId, requestBody);

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
                        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
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
    @GetMapping(value = {"/company/{company_number}/charges"})
    public ResponseEntity<ChargesApi> getCompanyCharges(
            @PathVariable("company_number") final String companyNumber,
            @RequestParam(value = "items_per_page", required = false) final Integer itemsPerPage,
            @RequestParam(value = "start_index", required = false) final Integer startIndex,
            @RequestParam(value = "filter", required = false) final String filter) {
        logger.debug(String.format("Started : getCompanyCharges Charges for Company Number %s ",
                companyNumber
        ));

        ResponseEntity<ChargesApi> chargeApiResponse = chargesService.findCharges(companyNumber,
                        new RequestCriteria()
                                .setItemsPerPage(itemsPerPage)
                                .setStartIndex(startIndex)
                                .setFilter(filter))
                .map(charges -> new ResponseEntity<>(charges, HttpStatus.OK))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        logger.debug(
                String.format("Finished : getCompanyCharges Charges found for Company Number %s ",
                        companyNumber));
        return chargeApiResponse;
    }

    /**
     * Delete a company charge id from company charges.
     *
     * @param companyNumber the company number for charges
     * @param chargeId      the charge information for a company
     */
    @DeleteMapping("/company/{company_number}/charges/{charge_id}")
    public ResponseEntity<Void> deleteCharge(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @PathVariable("charge_id") String chargeId) throws Exception {
        logger.info(String.format(
                "Payload Successfully received on DELETE with contextId %s and company number %s",
                contextId,
                companyNumber
        ));

        try {
            chargesService.deleteCharge(contextId, chargeId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (ResponseStatusException responseStatusException) {
            logger.error(String.format("Unexpected error occurred "
                            + "while processing DELETE request with contextId %s. "
                            + "Response Status Exception: %s.",
                    contextId, responseStatusException.getStatusCode().value()));
            return ResponseEntity.status(responseStatusException.getStatusCode()).build();
        } catch (Exception exception) {
            logger.error(String.format("Unexpected error occurred "
                            + "while processing DELETE request with contextId %s:"
                            + exception.getMessage(),
                    contextId));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

}
