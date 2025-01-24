package uk.gov.companieshouse.charges.data.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.charges.data.exception.SerDesException;
import uk.gov.companieshouse.charges.data.model.ResourceChangedRequest;
import uk.gov.companieshouse.charges.data.util.DateUtils;

@Component
public class ResourceChangedRequestMapper {

    private static final String CHANGED = "changed";
    private static final String DELETED = "deleted";
    private static final String RESOURCE_URI = "/company/%s/charges/%s";
    private static final String RESOURCE_KIND = "company-charges";

    private final Supplier<Instant> instantSupplier;
    private final ObjectMapper objectMapper;

    public ResourceChangedRequestMapper(Supplier<Instant> instantSupplier, ObjectMapper objectMapper) {
        this.instantSupplier = instantSupplier;
        this.objectMapper = objectMapper;
    }

    public ChangedResource mapChangedEvent(ResourceChangedRequest request) {
        return buildChangedResource(CHANGED, request);
    }

    public ChangedResource mapDeletedEvent(ResourceChangedRequest request) {
        ChangedResource changedResource = buildChangedResource(DELETED, request);
        try {
            Object dataAsObject = objectMapper.readValue(
                    objectMapper.writeValueAsString(request.data()), Object.class
            );
            changedResource.setDeletedData(dataAsObject);
        } catch (JsonProcessingException ex) {
            throw new SerDesException("Failed to serialise/deserialise data", ex);
        }
        return changedResource;
    }

    private ChangedResource buildChangedResource(final String type, ResourceChangedRequest request){
        ChangedResourceEvent event = new ChangedResourceEvent()
                .publishedAt(DateUtils.formatPublishedAt(this.instantSupplier.get()))
                .type(type);
        return new ChangedResource()
                .resourceUri(String.format(RESOURCE_URI, request.companyNumber(), request.chargeId()))
                .resourceKind(RESOURCE_KIND)
                .event(event)
                .contextId(request.contextId());
    }
}
