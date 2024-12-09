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
}
