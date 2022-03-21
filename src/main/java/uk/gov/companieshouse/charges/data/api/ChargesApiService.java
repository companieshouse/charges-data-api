package uk.gov.companieshouse.charges.data.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesApiService {

    private static final String CHANGED_RESOURCE_URI = "/resource-changed";
    public static final String RESOURCE_KIND = "company-charges";
    public static final String CHANGED_EVENT_TYPE = "changed";
    private final Logger logger;
    private final String chsKafkaUrl;
    private final ApiClientService apiClientService;

    /**
     * Invoke Charges API.
     */
    public ChargesApiService(@Value("chs.kafka.api.endpoint")String chsKafkaUrl,
                             ApiClientService apiClientService, Logger logger) {
        this.chsKafkaUrl = chsKafkaUrl;
        this.apiClientService = apiClientService;
        this.logger = logger;
    }

    /**
     * Call chs-kafka api.
     * @param companyNumber company charges number
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApi(String companyNumber) {
        InternalApiClient internalApiClient = apiClientService.getInternalApiClient();
        internalApiClient.setBasePath(chsKafkaUrl);

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        CHANGED_RESOURCE_URI, mapChangedResource(companyNumber));

        try {
            return changedResourcePost.execute();
        } catch (ApiErrorResponseException exp) {
            logger.error("Error occurred while calling /resource-changed endpoint", exp);
            throw new RuntimeException();
        }
    }

    private ChangedResource mapChangedResource(String companyNumber) {
        String resourceUri = String.format("/company/%s/company-charges", companyNumber);

        ChangedResourceEvent event = new ChangedResourceEvent();
        event.setType(CHANGED_EVENT_TYPE);
        ChangedResource changedResource = new ChangedResource();
        changedResource.setResourceUri(resourceUri);
        changedResource.event(event);
        changedResource.setResourceKind(RESOURCE_KIND);

        return changedResource;
    }

}
