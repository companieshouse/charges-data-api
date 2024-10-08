package uk.gov.companieshouse.charges.data.api;

import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;

public class ApiClientServiceImpl implements ApiClientService {

    private String apiKey;

    private String internalApiUrl;

    public ApiClientServiceImpl(String apiKey, String internalApiUrl) {
        this.apiKey = apiKey;
        this.internalApiUrl = internalApiUrl;
    }

    @Override
    public InternalApiClient getInternalApiClient() {
        InternalApiClient internalApiClient = new InternalApiClient(getHttpClient());
        internalApiClient.setInternalBasePath(internalApiUrl);
        internalApiClient.setBasePath(internalApiUrl);
        internalApiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());

        return internalApiClient;
    }

    private HttpClient getHttpClient() {
        ApiKeyHttpClient httpClient = new ApiKeyHttpClient(apiKey);
        return httpClient;
    }

}
