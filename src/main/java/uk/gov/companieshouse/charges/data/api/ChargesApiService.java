package uk.gov.companieshouse.charges.data.api;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesApiService {

    private static final String CHANGED_EVENT_TYPE = "changed";
    private static final String COMPANY_CHARGES_URI = "/company/%s/charges/%s";
    private final Logger logger;
    private final ChsKafkaApiClientServiceImpl apiClientServiceImpl;
    @Value("${charges.api.resource.changed.uri}")
    private String resourceChangedUri;
    @Value("${charges.api.resource.kind}")
    private String resourceKind;
    private static final String DELETE_EVENT_TYPE = "deleted";

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
     ** @param contextId contextId
     * @param companyNumber company charges number
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApi(String contextId, String companyNumber,
                                               String chargeId) {
        try {
            PrivateChangedResourcePost changedResourcePost = getChangedResourcePost(
                    resourceChangedUri,
                    mapChangedResource(contextId,
                    companyNumber, chargeId,
                    null));
            return changedResourcePost.execute();
        } catch (ApiErrorResponseException exp) {
            logger.error("Error occurred while calling /private/resource-changed endpoint.", exp);
            throw new ResponseStatusException(HttpStatus.valueOf(exp.getStatusCode()),
                    exp.getStatusMessage(), exp);
        }
    }

    private ChangedResource mapChangedResource(String contextId, String companyNumber,
                                               String chargeId, ChargeApi chargeApi) {
        ChangedResourceEvent event = new ChangedResourceEvent();
        event.setType(CHANGED_EVENT_TYPE);
        event.setPublishedAt(String.valueOf(OffsetDateTime.now()));
        ChangedResource changedResource = new ChangedResource();
        if (chargeApi != null) {
            changedResource.setDeletedData(chargeApi);
            event.setType(DELETE_EVENT_TYPE);
        }

        changedResource.setResourceUri(String.format(
                COMPANY_CHARGES_URI, companyNumber, chargeId));
        changedResource.event(event);
        changedResource.setResourceKind(resourceKind);
        changedResource.setContextId(contextId);

        return changedResource;
    }

    private PrivateChangedResourcePost getChangedResourcePost(String uri,
                                                              ChangedResource changedResource) {

        InternalApiClient internalApiClient = apiClientServiceImpl.getInternalApiClient();
        return internalApiClient.privateChangedResourceHandler()
                .postChangedResource(uri, changedResource);
    }

    /**
     * Call chs-kafka api.
     * @param contextId x-request-id
     * @param companyNumber companyNumber
     * @param chargeApi serialized json object
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApiWithDeleteEvent(String contextId,
                                                        String chargeId,
                                                        String companyNumber,
                                                        ChargeApi chargeApi) {
        try {

            PrivateChangedResourcePost changedResourcePost = getChangedResourcePost(
                    resourceChangedUri,
                    mapChangedResource(contextId, companyNumber,
                            chargeId, chargeApi));
            return changedResourcePost.execute();

        } catch (ApiErrorResponseException exp) {
            HttpStatus statusCode = HttpStatus.valueOf(exp.getStatusCode());
            logger.error("Unsuccessful call to /private/resource-changed "
                        + "endpoint for a charge delete event", exp);
            throw new ResponseStatusException(statusCode, exp.getMessage());
        }
    }

}
