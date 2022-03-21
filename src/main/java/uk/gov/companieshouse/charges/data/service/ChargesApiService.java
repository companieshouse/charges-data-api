package uk.gov.companieshouse.charges.data.service;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class ChargesApiService {

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

}
