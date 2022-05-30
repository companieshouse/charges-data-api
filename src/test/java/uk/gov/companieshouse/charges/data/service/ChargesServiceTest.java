package uk.gov.companieshouse.charges.data.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bson.Document;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.api.CompanyMetricsApiService;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class ChargesServiceTest {

    private static final String companyNumber = "NI622400";

    @Autowired
    private ObjectMapper mongoCustomConversions;

    private static ChargesRepository chargesRepository;

    private static CompanyMetricsApiService companyMetricsApiService;

    private static ChargesService chargesService;


    @Value("classpath:company-metrics-data.json")
    Resource metricsFile;

    @Value("classpath:charges-test-DB-record.json")
    Resource chargesFile;

    private static ChargesApiService chargesApiService;

    private static Logger logger;

    /**
     * Set up mocks and create the chargesService instance.
     * When using injects mocks and the @mock annotation the mock
     * in the service was not the same as the mock in the test so the when
     * was always returning null because it was a different mock instance.
     * Suspect this was due to sprint injection, not proven but
     * mock are now the same in test and serice to tests work and pass.
     */
    @BeforeAll
    public static void setup() {
        chargesApiService = mock(ChargesApiService.class);
        companyMetricsApiService = mock(CompanyMetricsApiService.class);
        ChargesTransformer chargesTransformer = mock(ChargesTransformer.class);
        chargesRepository = mock(ChargesRepository.class);
        logger = mock(Logger.class);
        chargesService = new ChargesService(logger, chargesRepository,
            chargesTransformer, chargesApiService, companyMetricsApiService);
    }

    /**
     * Reset the mocks so defaults are returned and invocation counters cleared.
     */
    @BeforeEach
    public void resetMocks() {
        reset(companyMetricsApiService);
        reset(chargesRepository);
    }

   @Test
    public void find_charges_should_return_charges() throws IOException {
        Pageable pageable = Pageable.ofSize(1);
        final PageImpl<ChargesDocument> page = new PageImpl<>(
                List.of(createCharges()));
        when(chargesRepository.findCharges(eq(companyNumber), any(Pageable.class)))
                .thenReturn(page);

        when(companyMetricsApiService.getCompanyMetrics(companyNumber))
                .thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
    }

    @Test
    public void empty_charges_when_repository_returns_empty_result() {
        var pageable = Pageable.ofSize(1);
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges.isPresent()).isFalse();
        verify(companyMetricsApiService, times(0))
                .getCompanyMetrics(companyNumber);
    }

    @Test
    public void empty_charges_when_company_metrics_returns_no_result() throws IOException {
        var pageable = Pageable.ofSize(1);
        var page = new PageImpl<>(
                List.of(createCharges()));
        when(chargesRepository.findCharges(eq(companyNumber), any(Pageable.class)))
                .thenReturn(page);

        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges.isPresent()).isFalse();
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

        try {
            chargesService.deleteCharge("0", "x-request-id", chargeId);
        }
        catch (ResponseStatusException statusException)  {
            Assert.assertEquals(HttpStatus.BAD_REQUEST, statusException.getStatus());
        }

        verify(chargesRepository, Mockito.times(0)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));
    }

    @Test
    void when_charge_id_does_exist_but_not_data_then_throws_IllegalArgumentExceptionException_error() {
        String chargeId = "CIrBNCKGlthNq2r9HzblXGKpT12";
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(populateChargesDocument(chargeId,null));

        try {
            chargesService.deleteCharge("0", "x-request-id", chargeId);
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
        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(
                populateChargesDocument(chargeId,populateCharge()));

        chargesService.deleteCharge("0","x-request-id", chargeId);

        verify(chargesRepository, Mockito.times(1)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));

    }

    @Test
    void when_charge_id_exist_then_invoke_chs_kafka_api_successfully_and_delete_charge() {
        String chargeId = "123456789"; String contextId="1111111";
        String companyNumber="0";

        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(
                populateChargesDocument(chargeId,populateCharge()));

        chargesService.deleteCharge(companyNumber,contextId, chargeId);

        verify(logger, Mockito.times(1)).info(
                "ChsKafka api invoked successfully for charge id " + chargeId +  " and x-request-id " + contextId
        );
        verify(chargesRepository, Mockito.times(1)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).findById(Mockito.eq(chargeId));
        verify(chargesApiService, times(1)).
                invokeChsKafkaApiWithDeleteEvent(eq(contextId), eq(chargeId), eq(companyNumber),eq(populateCharge()));

    }

    @Test
    void when_connection_issue_in_db_on_delete_then_throw_service_unavailable_exception() {
        String chargeId = "123456789";

        Mockito.when(chargesRepository.findById(chargeId)).thenReturn(
                populateChargesDocument(chargeId, populateCharge()));
        doThrow(new DataAccessResourceFailureException("Connection broken"))
                .when(chargesRepository).deleteById(chargeId);
        try {
                chargesService.deleteCharge("0", "x-request-id", chargeId);
        }
        catch (ResponseStatusException statusException)  {
            Assert.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, statusException.getStatus());
        }
    }

    @Test
    void when_connection_issue_in_db_on_find_in_delete_then_throw_service_unavailable_exception() {
        String chargeId = "123456789";

        doThrow(new DataAccessResourceFailureException("Connection broken"))
                .when(chargesRepository)
                .findById(chargeId);
        try {
            chargesService.deleteCharge("0", "x-request-id", chargeId);
        }
        catch (ResponseStatusException statusException)  {
            Assert.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, statusException.getStatus());
        }

    }

    private Optional<ChargesDocument> populateChargesDocument(String chargeId, ChargeApi chargeApi) {

        return Optional.of(new ChargesDocument()
                .setId(chargeId).setCompanyNumber("1234").setData(chargeApi)
                .setDeltaAt(LocalDateTime.now()));

    }
    private ChargeApi populateCharge() {

        ChargeApi chargeApi =  new ChargeApi();
        chargeApi.setId("12345"); chargeApi.setChargeCode("0");
        chargeApi.setChargeNumber(123);chargeApi.setEtag("1111111");
        return chargeApi;
    }

}
