package uk.gov.companieshouse.charges.data.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.*;

import org.bson.Document;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.api.CompanyMetricsApiService;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.RequestCriteria;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class ChargesServiceTest {

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

    /**
     * Reset the mocks so defaults are returned and invocation counters cleared.
     */
    @BeforeEach
    public void resetMocks() {
        chargesService = new ChargesService(logger, chargesRepository,
                chargesTransformer, chargesApiService, companyMetricsApiService);
    }

   @Test
    public void find_charges_should_return_charges() throws IOException {
        when(chargesRepository.findCharges(eq(companyNumber), any(), any(Pageable.class)))
                .thenReturn(Collections.singletonList(createCharges()));

        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
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
        when(chargesRepository.findCharges(eq(companyNumber), any(), any(Pageable.class)))
                .thenReturn(Collections.singletonList(createCharges()));

        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setFilter("outstanding").setStartIndex(0).setItemsPerPage(1));
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
        verify(chargesRepository).findCharges(companyNumber,
                Arrays.asList(ChargeApi.StatusEnum.SATISFIED, ChargeApi.StatusEnum.FULLY_SATISFIED),
                PageRequest.of(0,1));
    }

    @Test
    void findChargesWithFilterUnpaged() throws IOException {
        when(chargesRepository.findCharges(eq(companyNumber), any(), any(Pageable.class)))
                .thenReturn(Collections.singletonList(createCharges()));

        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setFilter("outstanding"));
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
        verify(chargesRepository).findCharges(companyNumber,
                Arrays.asList(ChargeApi.StatusEnum.SATISFIED, ChargeApi.StatusEnum.FULLY_SATISFIED), Pageable.unpaged());
    }

    @Test
    void findChargesWithSortedPageable() throws IOException {
        when(chargesRepository.findCharges(eq(companyNumber), any(), any(Pageable.class)))
                .thenReturn(Collections.singletonList(createCharges()));

        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setItemsPerPage(1).setStartIndex(0));
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
        verify(chargesRepository).findCharges(companyNumber, Collections.emptyList(),
                PageRequest.of(0, 1));
    }

    @Test
    void findChargesSortedWithoutPageable() throws IOException {
        when(chargesRepository.findCharges(eq(companyNumber), any(), any(Pageable.class)))
                .thenReturn(Collections.singletonList(createCharges()));

        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber, new RequestCriteria());
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
        verify(chargesRepository).findCharges(companyNumber, Collections.emptyList(), Pageable.unpaged());
    }

    @Test
    public void empty_charges_when_repository_returns_empty_result() {
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber, new RequestCriteria());
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getTotalCount()).isEqualTo(0);
        verify(companyMetricsApiService, times(1))
                .getCompanyMetrics(companyNumber);
    }

    @Test
    public void empty_charges_when_company_metrics_returns_no_result() throws IOException {
        when(chargesRepository.findCharges(eq(companyNumber), any(), any(Pageable.class)))
                .thenReturn(Collections.singletonList(createCharges()));

        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,
                new RequestCriteria().setItemsPerPage(1).setStartIndex(0));
        assertThat(charges.isPresent()).isTrue();
        //assert no metrics
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(0);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(0);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(0);
        assertEquals(charges.get().getItems().size(), charges.get().getTotalCount());
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

    @Test
    void when_charge_id_does_not_exist_then_throws_IllegalArgumentExceptionException_error() {
        String chargeId = "CIrBNCKGlthNq2r9HzblXGKpTrk";
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            chargesService.deleteCharge("x-request-id", chargeId);
        });

        assertEquals(HttpStatus.NOT_FOUND, ((ResponseStatusException)exception).getStatus());
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
            Assert.assertEquals(HttpStatus.NOT_FOUND, statusException.getStatus());
        }

        verify(chargesRepository, Mockito.times(0)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));
    }

    @Test
    void delete_charge_id_and_check_it_does_not_exist_in_database() throws Exception {
        String chargeId = "123456789";
        Mockito.when(chargesApiService.invokeChsKafkaApiWithDeleteEvent(anyString(), anyString(), anyString(), any())).thenReturn(new ApiResponse<>(200, null));
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
        Mockito.when(chargesApiService.invokeChsKafkaApiWithDeleteEvent(anyString(), anyString(), anyString(), any())).thenReturn(new ApiResponse<>(200, null));
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
            Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusException.getStatus());
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
            Assert.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, statusException.getStatus());
        }

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

    @Test
    void when_charge_id_exist_ani_invoke_chs_kafka_api_un_successfully_invoked_then_delete_charge() {
        String chargeId = "123456789"; String contextId="1111111"; String companyNumber="1234";
        Mockito.when(chargesApiService.invokeChsKafkaApiWithDeleteEvent(anyString(), anyString(), anyString(), any()))
                .thenReturn(new ApiResponse<>(301, null));
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(
                populateChargesDocument(chargeId,populateCharge()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> chargesService.deleteCharge(contextId, chargeId));

        verify(logger, Mockito.times(1)).info(
                "ChsKafka api DELETED invoked successfully for contextId "  + contextId + " and company number " + companyNumber
        );
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));
        verify(chargesRepository, Mockito.times(0)).deleteById(Mockito.any());
        verify(chargesApiService, times(1)).
                invokeChsKafkaApiWithDeleteEvent(eq(contextId), eq(chargeId), eq(companyNumber),eq(populateCharge()));

    }
}
