package uk.gov.companieshouse.charges.data.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.api.http.HttpClient;

@Component
public class ApiClientServiceImpl implements ApiClientService {

    @Value("${chs.kafka.api.key}")
    private String chsApiKey;

    @Value("${chs.kafka.api.endpoint}")
    private String internalApiUrl;

    @Override
    public InternalApiClient getInternalApiClient() {
        InternalApiClient internalApiClient = new InternalApiClient(getHttpClient());
        internalApiClient.setInternalBasePath(internalApiUrl);
        internalApiClient.setBasePath(internalApiUrl);

        return internalApiClient;
    }


    private HttpClient getHttpClient() {
        ApiKeyHttpClient httpClient = new ApiKeyHttpClient(chsApiKey);
        return httpClient;
    }
}
