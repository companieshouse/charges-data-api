package uk.gov.companieshouse.charges.data.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.api.charges.ChargeApi.AssetsCeasedReleasedEnum.PART_PROPERTY_RELEASED;
import static uk.gov.companieshouse.api.charges.ChargeApi.StatusEnum.PART_SATISFIED;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargeLink;
import uk.gov.companieshouse.api.charges.ClassificationApi;
import uk.gov.companieshouse.api.charges.InsolvencyCasesApi;
import uk.gov.companieshouse.api.charges.ParticularsApi;
import uk.gov.companieshouse.api.charges.PersonsEntitledApi;
import uk.gov.companieshouse.api.charges.ScottishAlterationsApi;
import uk.gov.companieshouse.api.charges.SecuredDetailsApi;
import uk.gov.companieshouse.api.charges.TransactionsApi;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.charges.data.model.ResourceChangedRequest;
import uk.gov.companieshouse.charges.data.util.DateUtils;


@ExtendWith(MockitoExtension.class)
class ResourceChangedRequestMapperTest {
    private static final String EXPECTED_CONTEXT_ID = "35234234";
    private static final String COMPANY_NUMBER = "12345678";
    private static final String CHARGE_ID = "ABCD8765EFGH4321";
    private static final String RESOURCE_URI = "/company/12345678/charges/ABCD8765EFGH4321";
    private static final String RESOURCE_KIND = "company-charges";
    private static final String CHANGED_EVENT = "changed";
    private static final String DELETED_EVENT = "deleted";
    private static final Instant UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final String PUBLISHED_AT = DateUtils.formatPublishedAt(UPDATED_AT);

    @Mock
    private Supplier<Instant> instantSupplier;
    @InjectMocks
    private ResourceChangedRequestMapper mapper;

    @Test
    void shouldMapChangedEvent() {
        // given
        ResourceChangedTestArgument argument = ResourceChangedTestArgument.builder()
                .withRequest(new ResourceChangedRequest(EXPECTED_CONTEXT_ID, CHARGE_ID, COMPANY_NUMBER,
                        null, false))
                .withContextId(EXPECTED_CONTEXT_ID)
                .withResourceUri(RESOURCE_URI)
                .withResourceKind(RESOURCE_KIND)
                .withEventType(CHANGED_EVENT)
                .withEventPublishedAt(PUBLISHED_AT)
                .build();
        when(instantSupplier.get()).thenReturn(UPDATED_AT);

        // when
        ChangedResource actual = mapper.mapChangedEvent(argument.request());

        // then
        assertEquals(argument.changedResource(), actual);
    }

    @ParameterizedTest
    @MethodSource("resourceChangedScenarios")
    void shouldMapDeletedEvent(ResourceChangedTestArgument argument) {
        // given
        when(instantSupplier.get()).thenReturn(UPDATED_AT);

        // when
        ChangedResource actual = mapper.mapDeletedEvent(argument.request());

        // then
        assertEquals(argument.changedResource(), actual);
    }

    static Stream<ResourceChangedTestArgument> resourceChangedScenarios() {
        return Stream.of(
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest(EXPECTED_CONTEXT_ID, CHARGE_ID, COMPANY_NUMBER,
                                null, true))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri(RESOURCE_URI)
                        .withResourceKind(RESOURCE_KIND)
                        .withEventType(DELETED_EVENT)
                        .withEventPublishedAt(PUBLISHED_AT)
                        .build(),
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest(EXPECTED_CONTEXT_ID, CHARGE_ID, COMPANY_NUMBER,
                                getCharges(), true))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri(RESOURCE_URI)
                        .withResourceKind(RESOURCE_KIND)
                        .withEventType(DELETED_EVENT)
                        .withDeletedData(getCharges())
                        .withEventPublishedAt(PUBLISHED_AT)
                        .build()
        );
    }

    record ResourceChangedTestArgument(ResourceChangedRequest request, ChangedResource changedResource) {

        public static ResourceChangedTestArgumentBuilder builder() {
                return new ResourceChangedTestArgumentBuilder();
            }

            @Override
            public String toString() {
                return this.request.toString();
            }
        }

    static class ResourceChangedTestArgumentBuilder {
        private ResourceChangedRequest request;
        private String resourceUri;
        private String resourceKind;
        private String contextId;
        private String eventType;
        private String eventPublishedAt;
        private Object deletedData;

        public ResourceChangedTestArgumentBuilder withRequest(ResourceChangedRequest request) {
            this.request = request;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withResourceUri(String resourceUri) {
            this.resourceUri = resourceUri;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withResourceKind(String resourceKind) {
            this.resourceKind = resourceKind;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withContextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withEventPublishedAt(String eventPublishedAt) {
            this.eventPublishedAt = eventPublishedAt;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withDeletedData(Object deletedData) {
            this.deletedData = deletedData;
            return this;
        }

        public ResourceChangedTestArgument build() {
            ChangedResource changedResource = new ChangedResource();
            changedResource.setResourceUri(this.resourceUri);
            changedResource.setResourceKind(this.resourceKind);
            changedResource.setContextId(this.contextId);
            ChangedResourceEvent event = new ChangedResourceEvent();
            event.setType(this.eventType);
            event.setPublishedAt(this.eventPublishedAt);
            changedResource.setEvent(event);
            changedResource.setDeletedData(deletedData);
            return new ResourceChangedTestArgument(this.request, changedResource);
        }
    }

    private static ChargeApi getCharges() {
        ChargeApi charges = new ChargeApi();
        ClassificationApi classificationApi = new ClassificationApi();
        ParticularsApi particularsApi = new ParticularsApi();
        SecuredDetailsApi securedDetailsApi = new SecuredDetailsApi();
        ScottishAlterationsApi scottishAlterationsApi = new ScottishAlterationsApi();
        List<PersonsEntitledApi> personsEntitled = new ArrayList<>();
        List<TransactionsApi> transactions = new ArrayList<>();
        List<InsolvencyCasesApi> insolvencyCases = new ArrayList<>();
        ChargeLink chargeLink = new ChargeLink();
        charges.setEtag("etag");
        charges.setId("id");
        charges.setChargeCode("chargeCode");
        charges.setClassification(classificationApi);
        charges.setChargeNumber(22);
        charges.setStatus(PART_SATISFIED);
        charges.setAssetsCeasedReleased(PART_PROPERTY_RELEASED);
        charges.setAcquiredOn(null);
        charges.setDeliveredOn(null);
        charges.setResolvedOn(null);
        charges.setCoveringInstrumentDate(null);
        charges.createdOn(null);
        charges.setSatisfiedOn(null);
        charges.setParticulars(particularsApi);
        charges.setSecuredDetails(securedDetailsApi);
        charges.setScottishAlterations(scottishAlterationsApi);
        charges.setMoreThanFourPersonsEntitled(false);
        charges.setPersonsEntitled(personsEntitled);
        charges.setTransactions(transactions);
        charges.setInsolvencyCases(insolvencyCases);
        charges.setLinks(chargeLink);
        return charges;
    }
}
