package uk.gov.companieshouse.charges.data.api;

import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesApiService {

    private static final String CHANGED_EVENT_TYPE = "changed";
    private static final String COMPANY_CHARGES_URI = "/company/%s/company-charges";
    private final Logger logger;
    private final ChsKafkaApiClientServiceImpl apiClientServiceImpl;
    @Value("${charges.api.resource.changed.uri}")
    private String resourceChangedUri;
    @Value("${charges.api.resource.kind}")
    private String resourceKind;

    /**
     * Invoke Charges API.
     */
    @Autowired
    public ChargesApiService(ChsKafkaApiClientServiceImpl apiClientService, Logger logger) {
        this.apiClientServiceImpl = apiClientService;
        this.logger = logger;
    }

    /**
     * Call chs-kafka api.
     *
     * @param companyNumber company charges number
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApi(String contextId, String companyNumber) {
        InternalApiClient internalApiClient = apiClientServiceImpl.getInternalApiClient();

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        resourceChangedUri, mapChangedResource(contextId, companyNumber));

        try {
            return changedResourcePost.execute();
        } catch (ApiErrorResponseException exp) {
            logger.error(String.format(
                    "Error occurred while calling /resource-changed endpoint. "
                    + "Message: %s StackTrace: ",
                    exp.getMessage(), exp.getStackTrace().toString()));
            throw new ResponseStatusException(HttpStatus.valueOf(exp.getStatusCode()),
                    exp.getStatusMessage(), exp);
        }
    }

    private ChangedResource mapChangedResource(String contextId, String companyNumber) {
        String resourceUri = String.format(COMPANY_CHARGES_URI, companyNumber);

        ChangedResourceEvent event = new ChangedResourceEvent();
        event.setType(CHANGED_EVENT_TYPE);
        event.setPublishedAt(String.valueOf(OffsetDateTime.now()));
        ChangedResource changedResource = new ChangedResource();
        changedResource.setResourceUri(resourceUri);
        changedResource.event(event);
        changedResource.setResourceKind(resourceKind);
        changedResource.setContextId(contextId);

        return changedResource;
    }

}
