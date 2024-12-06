package uk.gov.companieshouse.charges.data.api;

import static uk.gov.companieshouse.charges.data.ChargesDataApiApplication.NAMESPACE;

import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.charges.data.exception.ServiceUnavailableException;
import uk.gov.companieshouse.charges.data.model.ResourceChangedRequest;
import uk.gov.companieshouse.charges.data.transform.ResourceChangedRequestMapper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class ChargesApiService {

    private static final String RESOURCE_CHANGED_URI = "/private/resource-changed";
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final Supplier<InternalApiClient> internalApiClientSupplier;
    private final ResourceChangedRequestMapper mapper;


    /**
     * Invoke Charges API.
     */

    public ChargesApiService(Supplier<InternalApiClient> internalApiClientSupplier, ResourceChangedRequestMapper mapper) {
        this.internalApiClientSupplier = internalApiClientSupplier;
        this.mapper = mapper;
    }


    /**
     * Calls the CHS Kafka api.
     * @param resourceChangedRequest encapsulates details relating to the updated or deleted company exemption
     * @return The service status of the response from chs kafka api
     */
    public void invokeChsKafkaApi(ResourceChangedRequest resourceChangedRequest) {
        InternalApiClient internalApiClient = internalApiClientSupplier.get();

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        RESOURCE_CHANGED_URI, mapper.mapChangedEvent(resourceChangedRequest));
        try {
            changedResourcePost.execute();
        } catch (ApiErrorResponseException ex) {
            LOGGER.info("Resource changed call failed: %s".formatted(ex.getStatusCode()));
            throw new ServiceUnavailableException("Error calling resource changed endpoint");
        }
    }

    public void invokeChsKafkaApiDelete(ResourceChangedRequest resourceChangedRequest) {
        InternalApiClient internalApiClient = internalApiClientSupplier.get();

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        RESOURCE_CHANGED_URI, mapper.mapDeletedEvent(resourceChangedRequest));
        try {
            changedResourcePost.execute();
        } catch (ApiErrorResponseException ex) {
            LOGGER.info("Resource changed call failed: %s".formatted(ex.getStatusCode()));
            throw new ServiceUnavailableException("Error calling resource changed endpoint");
        }
    }

    /**
     * Call chs-kafka api.
     * * @param contextId contextId
     *
     * @param companyNumber company charges number
     * @return response returned from chs-kafka api
     */
    /*public ApiResponse<Void> invokeChsKafkaApi(String contextId, String companyNumber,
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
        event.setPublishedAt(DateUtils.formatPublishedAt(Instant.now()));
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
    }*/

    /**
     * Call chs-kafka api.
     *
     * @param contextId     x-request-id
     * @param companyNumber companyNumber
     * @param chargeApi     serialized json object
     * @return response returned from chs-kafka api
     */
    /*public ApiResponse<Void> invokeChsKafkaApiWithDeleteEvent(String contextId,
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
    }*/

}
