package uk.gov.companieshouse.charges.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesService {

    private final Logger logger;
    private ChargesTransformer chargesTransformer;

    @Autowired
    public ChargesService(final Logger logger) {
        this.logger = logger;
        this.chargesTransformer = new ChargesTransformer(logger);
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
     *
     * @param companyNumber company number for charge.
     * @param chargeId      charges Id.
     * @param requestBody   request body.
     */
    public void upsertCharges(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        logger.debug(String.format("Save or Update charge %s with company number %s ", chargeId,
                companyNumber));


        ChargesDocument charges =
                this.chargesTransformer.transform(companyNumber, chargeId, requestBody);

        //TODO save charges
    }


}
