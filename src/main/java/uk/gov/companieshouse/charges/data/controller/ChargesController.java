package uk.gov.companieshouse.charges.data.controller;

import static uk.gov.companieshouse.charges.data.ChargesDataApiApplication.NAMESPACE;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;
import uk.gov.companieshouse.charges.data.model.RequestCriteria;
import uk.gov.companieshouse.charges.data.service.ChargesService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;


@RestController
public class ChargesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    ChargesService chargesService;

    @Autowired
    public ChargesController(final ChargesService chargesService) {
        this.chargesService = chargesService;
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
        DataMapHolder.get().companyNumber(companyNumber);
        DataMapHolder.get().mortgageId(chargeId);
        LOGGER.info("Upserting company charges", DataMapHolder.getLogMap());

        chargesService.upsertCharges(contextId, companyNumber, chargeId, requestBody);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a company charge id from company charges.
     *
     * @param companyNumber the company number for charges
     * @param chargeId      the charge information for a company
     */
    @DeleteMapping("/company/{company_number}/charge/{charge_id}/internal")
    public ResponseEntity<Void> deleteCharge(
            @RequestHeader("x-request-id") String contextId, @PathVariable("company_number") String companyNumber,
            @PathVariable("charge_id") String chargeId, @RequestHeader("X-DELTA-AT") String deltaAt) {
        DataMapHolder.get().companyNumber(companyNumber);
        DataMapHolder.get().mortgageId(chargeId);
        LOGGER.info("Deleting company charge", DataMapHolder.getLogMap());

        chargesService.deleteCharge(contextId, companyNumber, chargeId, deltaAt);
        return ResponseEntity.ok().build();
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
        DataMapHolder.get().companyNumber(companyNumber);
        DataMapHolder.get().mortgageId(chargeId);
        LOGGER.info("Getting company charge details", DataMapHolder.getLogMap());
        return ResponseEntity.ok().body(chargesService.getChargeDetails(companyNumber, chargeId));
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
        DataMapHolder.get().companyNumber(companyNumber);
        LOGGER.info("Getting all charges for company", DataMapHolder.getLogMap());
        return ResponseEntity.ok().body(chargesService.findCharges(companyNumber,
                new RequestCriteria().setItemsPerPage(itemsPerPage).setStartIndex(startIndex).setFilter(filter)));
    }
}
