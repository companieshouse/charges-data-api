package uk.gov.companieshouse.charges.data.api;

import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.api.http.HttpClient;

public class ApiClientServiceImpl implements ApiClientService {

    private final String apiKey;

    private final String internalApiUrl;

    public ApiClientServiceImpl(String apiKey, String internalApiUrl) {
        this.apiKey = apiKey;
        this.internalApiUrl = internalApiUrl;
    }

    @Override
    public InternalApiClient getInternalApiClient() {
        InternalApiClient internalApiClient = new InternalApiClient(getHttpClient());
        internalApiClient.setInternalBasePath(internalApiUrl);
        internalApiClient.setBasePath(internalApiUrl);

        return internalApiClient;
    }

    private HttpClient getHttpClient() {
        return new ApiKeyHttpClient(apiKey);
    }

}
