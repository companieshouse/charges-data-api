package uk.gov.companieshouse.charges.data.service;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.data.requests.ChargesRequest;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesService {

    private final Logger logger;

    public ChargesService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Invoke Charges API.
     */
    public ApiResponse<?> invokeChargesApi() {
        InternalApiClient internalApiClient = getInternalApiClient();
        internalApiClient.setBasePath("apiUrl");

        return null;
    }

    @Lookup
    public InternalApiClient getInternalApiClient() {
        return null;
    }

    /**
     * Save or Update charges.
     * @param companyNumber company number for charge.
     * @param chargeId charges Id.
     * @param requestBody request body.
     */
    public void saveOrUpdateCharges(String companyNumber, String chargeId, ChargesRequest requestBody) {
        logger.debug(String.format("Save or Update charge %s with company number %s ", chargeId,
                companyNumber));
        // TODO Save to database
    }
}
