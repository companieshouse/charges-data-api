package uk.gov.companieshouse.charges.data.api;

import static uk.gov.companieshouse.charges.data.ChargesDataApiApplication.NAMESPACE;

import java.time.Instant;
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
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;
import uk.gov.companieshouse.charges.data.util.DateTimeFormatter;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class ChargesApiService {

    private static final String CHANGED_EVENT_TYPE = "changed";
    private static final String COMPANY_CHARGES_URI = "/company/%s/charges/%s";
    private static final String DELETE_EVENT_TYPE = "deleted";

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final ChsKafkaApiClientServiceImpl apiClientServiceImpl;
    @Value("${charges.api.resource.changed.uri}")
    private String resourceChangedUri;
    @Value("${charges.api.resource.kind}")
    private String resourceKind;

    /**
     * Invoke Charges API.
     */
    @Autowired
    public ChargesApiService(ChsKafkaApiClientServiceImpl apiClientService) {
        this.apiClientServiceImpl = apiClientService;
    }

    /**
     * Call chs-kafka api.
     * * @param contextId contextId
     *
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
            LOGGER.error("Error occurred while calling /private/resource-changed endpoint.", exp,
                    DataMapHolder.getLogMap());
            throw new ResponseStatusException(HttpStatus.valueOf(exp.getStatusCode()),
                    exp.getStatusMessage(), exp);
        }
    }

    private ChangedResource mapChangedResource(String contextId, String companyNumber,
            String chargeId, ChargeApi chargeApi) {
        ChangedResourceEvent event = new ChangedResourceEvent();
        event.setType(CHANGED_EVENT_TYPE);
        event.setPublishedAt(DateTimeFormatter.formatPublishedAt(Instant.now()));
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
     *
     * @param contextId     x-request-id
     * @param companyNumber companyNumber
     * @param chargeApi     serialized json object
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApiWithDeleteEvent(String contextId,
            String chargeId,
            String companyNumber,
            ChargeApi chargeApi) {
        try {
            return getChangedResourcePost(resourceChangedUri,
                    mapChangedResource(contextId, companyNumber, chargeId, chargeApi))
                    .execute();

        } catch (ApiErrorResponseException exp) {
            LOGGER.error("Unsuccessful call to /private/resource-changed "
                    + "endpoint for a charge delete event", exp, DataMapHolder.getLogMap());
            throw new ResponseStatusException(HttpStatus.valueOf(exp.getStatusCode()), exp.getMessage());
        }
    }

}
