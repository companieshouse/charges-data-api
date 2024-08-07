package uk.gov.companieshouse.charges.data.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.api.charges.ChargeApi.AssetsCeasedReleasedEnum.PART_PROPERTY_RELEASED;
import static uk.gov.companieshouse.api.charges.ChargeApi.StatusEnum.PART_SATISFIED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargeLink;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.ClassificationApi;
import uk.gov.companieshouse.api.charges.InsolvencyCasesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.charges.InternalData;
import uk.gov.companieshouse.api.charges.ParticularsApi;
import uk.gov.companieshouse.api.charges.PersonsEntitledApi;
import uk.gov.companieshouse.api.charges.ScottishAlterationsApi;
import uk.gov.companieshouse.api.charges.SecuredDetailsApi;
import uk.gov.companieshouse.api.charges.TransactionsApi;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.api.CompanyMetricsApiService;
import uk.gov.companieshouse.charges.data.model.ChargesAggregate;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.RequestCriteria;
import uk.gov.companieshouse.charges.data.model.TotalCharges;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class ChargesServiceTest {

    private static final String companyNumber = "NI622400";

    @Autowired
    private ObjectMapper mongoCustomConversions;

    @Mock
    private ChargesRepository chargesRepository;

    @Mock
    private CompanyMetricsApiService companyMetricsApiService;

    private ChargesService chargesService;

    @Value("classpath:company-metrics-data.json")
    Resource metricsFile;

    @Value("classpath:charges-test-DB-record.json")
    Resource chargesFile;

    @Mock
    private ChargesApiService chargesApiService;

    @Mock
    private Logger logger;

    @Mock
    private ChargesTransformer chargesTransformer;

    @Mock
    private ChargesAggregate chargesAggregate;

    @Mock
    private ChargesDocument document;

    private final InternalChargeApi internalChargeApi = buildInternalCharges();

    /**
     * Reset the mocks so defaults are returned and invocation counters cleared.
     */
    @BeforeEach
    public void resetMocks() {
        chargesService = new ChargesService(logger, chargesRepository,
                chargesTransformer, chargesApiService, companyMetricsApiService);
    }

   @Test
    void find_charges_should_return_charges() throws IOException {
        trainMocks();
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setItemsPerPage(1).setStartIndex(0));
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
    }

    @Test
    void findChargesWithFilter() throws IOException {
        trainMocks();
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setFilter("outstanding").setStartIndex(0).setItemsPerPage(1));
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
        verify(chargesRepository).findCharges(companyNumber,
                Arrays.asList(ChargeApi.StatusEnum.SATISFIED.toString(), ChargeApi.StatusEnum.FULLY_SATISFIED.toString()), 0, 1);
    }

    @Test
    void findChargesWithFilterUnpaged() throws IOException {
        trainMocks();
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setFilter("outstanding"));
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
        verify(chargesRepository).findCharges(companyNumber,
                Arrays.asList(ChargeApi.StatusEnum.SATISFIED.toString(), ChargeApi.StatusEnum.FULLY_SATISFIED.toString()), 0, 25);
    }

    @Test
    void findChargesWithPaging() throws IOException {
        trainMocks();
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setItemsPerPage(1).setStartIndex(0));
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
        verify(chargesRepository).findCharges(companyNumber, Collections.emptyList(), 0, 1);
    }

    @Test
    void findChargesWithoutPaging() throws IOException {
        trainMocks();
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber, new RequestCriteria());
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
        verify(chargesRepository).findCharges(companyNumber, Collections.emptyList(), 0, 25);
    }

    @Test
    void findChargesWithPageSizeAboveLimit() throws IOException {
        trainMocks();
        chargesService.findCharges(companyNumber, new RequestCriteria().setItemsPerPage(101).setStartIndex(0));
        verify(chargesRepository).findCharges(companyNumber, Collections.emptyList(), 0, 100);
    }

    @Test
    void findChargesWithFilterNoResults() throws IOException {
        when(chargesRepository.findCharges(eq(companyNumber), any(), anyInt(), anyInt()))
                .thenReturn(new ChargesAggregate());
        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> chargeApi = chargesService.findCharges(companyNumber, new RequestCriteria().setFilter("outstanding"));
        assertEquals(0, chargeApi.get().getTotalCount());
        verify(chargesRepository).findCharges(companyNumber,
                Arrays.asList(ChargeApi.StatusEnum.SATISFIED.toString(), ChargeApi.StatusEnum.FULLY_SATISFIED.toString()), 0, 25);
    }

    @Test
    void findChargesWithNoResults() throws IOException {
        when(chargesRepository.findCharges(eq(companyNumber), any(), anyInt(), anyInt()))
                .thenReturn(new ChargesAggregate());
        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> chargeApi = chargesService.findCharges(companyNumber, new RequestCriteria());
        assertEquals(0, chargeApi.get().getTotalCount());
        verify(chargesRepository).findCharges(companyNumber, emptyList(), 0, 25);
    }

    @Test
     void empty_charges_when_repository_returns_empty_result() {
        when(chargesRepository.findCharges(anyString(), any(), anyInt(), anyInt())).thenReturn(chargesAggregate);
        when(chargesAggregate.getChargesDocuments()).thenReturn(singletonList(document));
        when(chargesAggregate.getTotalCharges()).thenReturn(singletonList(new TotalCharges(0L)));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber, new RequestCriteria());
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getTotalCount()).isEqualTo(0);
        verify(companyMetricsApiService, times(1))
                .getCompanyMetrics(companyNumber);
    }

    @Test
     void empty_charges_when_company_metrics_returns_no_result() {
        when(chargesRepository.findCharges(anyString(), any(), anyInt(), anyInt())).thenReturn(chargesAggregate);
        when(chargesAggregate.getChargesDocuments()).thenReturn(singletonList(document));
        when(chargesAggregate.getTotalCharges()).thenReturn(singletonList(new TotalCharges(1L)));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setItemsPerPage(1).setStartIndex(0));
        assertThat(charges.isPresent()).isTrue();
        //assert no metrics
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(0);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(0);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(0);
        assertEquals(charges.get().getItems().size(), charges.get().getTotalCount());
    }

    @Test
    void when_charge_id_does_not_exist_then_throws_IllegalArgumentExceptionException_error() {
        String chargeId = "CIrBNCKGlthNq2r9HzblXGKpTrk";
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            chargesService.deleteCharge("x-request-id", chargeId);
        });

        assertEquals(HttpStatus.NOT_FOUND, ((ResponseStatusException)exception).getStatusCode());
        verify(chargesRepository, Mockito.times(0)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));
    }

    @Test
    void when_charge_id_does_exist_but_not_data_then_throws_IllegalArgumentExceptionException_error() {
        String chargeId = "CIrBNCKGlthNq2r9HzblXGKpT12";
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(populateChargesDocument(chargeId,null));

        try {
            chargesService.deleteCharge( "x-request-id", chargeId);
        }
        catch (ResponseStatusException statusException)  {
            assertEquals(HttpStatus.NOT_FOUND, statusException.getStatusCode());
        }

        verify(chargesRepository, Mockito.times(0)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));
    }

    @Test
    void delete_charge_id_and_check_it_does_not_exist_in_database() {
        String chargeId = "123456789";
        Mockito.when(chargesApiService.invokeChsKafkaApiWithDeleteEvent(anyString(), anyString(), anyString(), any()))
                .thenReturn(new ApiResponse<>(200, null));
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(
                populateChargesDocument(chargeId,populateCharge()));

        chargesService.deleteCharge("x-request-id", chargeId);

        verify(chargesRepository, Mockito.times(1)).deleteById(Mockito.any());
        verify(chargesApiService, Mockito.times(1)).invokeChsKafkaApiWithDeleteEvent(anyString(), anyString(), anyString(), any());
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));

    }

    @Test
    void when_charge_id_exist_ani_invoke_chs_kafka_api_successfully_invoked_then_delete_charge() {
        String chargeId = "123456789"; String contextId="1111111"; String companyNumber="1234";
        Mockito.when(chargesApiService.invokeChsKafkaApiWithDeleteEvent(anyString(), anyString(), anyString(), any()))
                        .thenReturn(new ApiResponse<>(200, null));
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(
                populateChargesDocument(chargeId,populateCharge()));

        chargesService.deleteCharge(contextId, chargeId);

        verify(logger, Mockito.times(1)).info(
                    "ChsKafka api DELETED invoked successfully for contextId "  + contextId + " and company number " + companyNumber
        );
        verify(chargesRepository, Mockito.times(1)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));
        verify(chargesApiService, times(1)).
                invokeChsKafkaApiWithDeleteEvent(eq(contextId), eq(chargeId), eq(companyNumber),eq(populateCharge()));

    }

    @Test
    void when_connection_issue_in_db_on_delete_then_throw_service_internal_exception() {
        String chargeId = "123456789";

        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(
                populateChargesDocument(chargeId, populateCharge()));

        try {
                chargesService.deleteCharge("x-request-id", chargeId);
        }
        catch (ResponseStatusException statusException)  {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusException.getStatusCode());
        }
    }

    @Test
    void when_connection_issue_in_db_on_find_in_delete_then_throw_service_unavailable_exception() {
        String chargeId = "123456789";

        doThrow(new DataAccessResourceFailureException("Connection broken"))
                .when(chargesRepository)
                .findById(chargeId);
        try {
            chargesService.deleteCharge( "x-request-id", chargeId);
        }
        catch (ResponseStatusException statusException)  {
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, statusException.getStatusCode());
        }

    }

    @Test
    void when_charge_id_exist_ani_invoke_chs_kafka_api_un_successfully_invoked_then_delete_charge() {
        String chargeId = "123456789"; String contextId="1111111"; String companyNumber="1234";
        Mockito.when(chargesApiService.invokeChsKafkaApiWithDeleteEvent(anyString(), anyString(), anyString(), any()))
                .thenReturn(new ApiResponse<>(301, null));
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(
                populateChargesDocument(chargeId,populateCharge()));

        assertThrows(ResponseStatusException.class, () -> chargesService.deleteCharge(contextId, chargeId));

        verify(logger, Mockito.times(1)).info(
                "ChsKafka api DELETED invoked successfully for contextId "  + contextId + " and company number " + companyNumber
        );
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));
        verify(chargesRepository, Mockito.times(0)).deleteById(Mockito.any());
        verify(chargesApiService, times(1)).
                invokeChsKafkaApiWithDeleteEvent(eq(contextId), eq(chargeId), eq(companyNumber),eq(populateCharge()));

    }


    @Test
    void testInsertChargeHandlesCompensatoryTransactionWhenServiceUnavailableThrown() {
        // given
        String chargeId = "123456789";
        String contextId = "1111111";
        String companyNumber = "1234";
        ChargesDocument chargesDocument = new ChargesDocument()
                .setId("123456789")
                .setCompanyNumber("1234");

        when(chargesTransformer.transform(any(), any(), any(InternalChargeApi.class))).thenReturn(
                chargesDocument);
        when(chargesRepository.findById(any())).thenReturn(Optional.empty());
        when(chargesApiService.invokeChsKafkaApi(any(), any(), any())).thenThrow(
                ResponseStatusException.class);

        // When
        Executable executable = () -> chargesService.upsertCharges(contextId, companyNumber,
                chargeId, internalChargeApi);

        // then
        assertThrows(ResponseStatusException.class, executable);
        verify(chargesRepository).save(chargesDocument);
        verify(chargesRepository).deleteById(chargesDocument.getId());
    }

    @Test
    void testUpdateChargeHandlesCompensatoryTransactionWhenServiceUnavailableThrown() {
        // given
        String chargeId = "123456789";
        String contextId = "1111111";
        String companyNumber = "1234";
        internalChargeApi.getInternalData()
                .setDeltaAt(OffsetDateTime.parse("2023-11-06T15:30:00.000000Z"));
        ChargesDocument deltaChargesDocument = new ChargesDocument().setId("chargesIdDELTA")
                .setCompanyNumber("012345678")
                .setDeltaAt(OffsetDateTime.parse("2023-11-06T16:30:00.000000Z"));

        ChargesDocument existingDocument = new ChargesDocument().setId("chargesIdEXISTING")
                .setCompanyNumber("012345678")
                .setDeltaAt(OffsetDateTime.parse("2023-11-06T12:00:00.000000Z"));

        when(chargesTransformer.transform(any(), any(), any(InternalChargeApi.class))).thenReturn(
                deltaChargesDocument);
        when(chargesRepository.findById(any())).thenReturn(Optional.of(existingDocument));
        when(chargesApiService.invokeChsKafkaApi(any(), any(), any())).thenThrow(
                ResponseStatusException.class);

        // When
        Executable executable = () -> chargesService.upsertCharges(contextId, companyNumber,
                chargeId, internalChargeApi);

        // then
        assertThrows(ResponseStatusException.class, executable);
        verify(chargesRepository).save(deltaChargesDocument);
        verify(chargesRepository).save(existingDocument);
    }


    private Optional<ChargesDocument> populateChargesDocument(String chargeId, ChargeApi chargeApi) {

        return Optional.of(new ChargesDocument()
                .setId(chargeId).setCompanyNumber("1234").setData(chargeApi)
                .setDeltaAt(OffsetDateTime.now()));

    }
    private ChargeApi populateCharge() {

        ChargeApi chargeApi =  new ChargeApi();
        chargeApi.setId("12345"); chargeApi.setChargeCode("0");
        chargeApi.setChargeNumber(123);chargeApi.setEtag("1111111");
        return chargeApi;
    }

    private ChargesDocument createCharges() throws IOException {
        Document chargesBson = readData(chargesFile);
        return mongoCustomConversions.convertValue(chargesBson, ChargesDocument.class);
    }

    private MetricsApi createMetrics() throws IOException {
        Document chargesBson = readData(metricsFile);
        return mongoCustomConversions.convertValue(chargesBson, MetricsApi.class);
    }

    private Document readData(Resource resource) throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                resource.getInputStream())));
        return Document.parse(data);
    }

    private void trainMocks() throws IOException {
        when(chargesRepository.findCharges(eq(companyNumber), any(), anyInt(), anyInt()))
                .thenReturn(new ChargesAggregate(singletonList(new TotalCharges(1L)), singletonList(createCharges())));
        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
    }

    private InternalChargeApi buildInternalCharges() {
        InternalChargeApi output = new InternalChargeApi();

        ChargeApi externalData = new ChargeApi();
        ClassificationApi classificationApi = new ClassificationApi();
        ParticularsApi particularsApi = new ParticularsApi();
        SecuredDetailsApi securedDetailsApi = new SecuredDetailsApi();
        ScottishAlterationsApi scottishAlterationsApi = new ScottishAlterationsApi();
        List<PersonsEntitledApi> personsEntitled = new ArrayList<>();
        List<TransactionsApi> transactions = new ArrayList<>();
        List<InsolvencyCasesApi> insolvencyCases = new ArrayList<>();
        ChargeLink chargeLink = new ChargeLink();
        externalData.setEtag("etag");
        externalData.setId("id");
        externalData.setChargeCode("chargeCode");
        externalData.setClassification(classificationApi);
        externalData.setChargeNumber(22);
        externalData.setStatus(PART_SATISFIED);
        externalData.setAssetsCeasedReleased(PART_PROPERTY_RELEASED);
        externalData.setAcquiredOn(null);
        externalData.setDeliveredOn(null);
        externalData.setResolvedOn(null);
        externalData.setCoveringInstrumentDate(null);
        externalData.createdOn(null);
        externalData.setSatisfiedOn(null);
        externalData.setParticulars(particularsApi);
        externalData.setSecuredDetails(securedDetailsApi);
        externalData.setScottishAlterations(scottishAlterationsApi);
        externalData.setMoreThanFourPersonsEntitled(false);
        externalData.setPersonsEntitled(personsEntitled);
        externalData.setTransactions(transactions);
        externalData.setInsolvencyCases(insolvencyCases);
        externalData.setLinks(chargeLink);

        InternalData internalData = new InternalData();
        internalData.setDeltaAt(OffsetDateTime.parse("2022-01-13T00:00:00Z"));
        internalData.setUpdatedBy("updatedBy");
        output.setExternalData(externalData);
        output.setInternalData(internalData);

        return output;
    }
}
