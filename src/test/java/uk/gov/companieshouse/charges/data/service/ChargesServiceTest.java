package uk.gov.companieshouse.charges.data.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.api.CompanyMetricsApiService;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.Updated;
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
        ChargesApiService chargesApiService = mock(ChargesApiService.class);
        companyMetricsApiService = mock(CompanyMetricsApiService.class);
        ChargesTransformer chargesTransformer = mock(ChargesTransformer.class);
        chargesRepository = mock(ChargesRepository.class);
        Logger logger = mock(Logger.class);
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
        Mockito.when(chargesRepository.existsById(chargeId)).thenReturn(false);

        Assert.assertThrows(IllegalArgumentException.class, () ->
                chargesService.deleteCharge("x-request-id", chargeId));

        verify(chargesRepository, Mockito.times(0)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).existsById(Mockito.eq(chargeId));
    }

    @Test
    void delete_charge_id_and_check_it_does_not_exist_in_database() throws Exception {
        String chargeId = "123456789";
            var chargesDocument = Optional.of(new ChargesDocument()
                .setId(chargeId).setCompanyNumber("1234").setData(new ChargeApi())
                .setDeltaAt(LocalDateTime.now()));
        Mockito.when(chargesRepository.existsById(chargeId)).thenReturn(true);

        chargesService.deleteCharge("x-request-id", chargeId);

        verify(chargesRepository, Mockito.times(1)).deleteById(Mockito.any());
        verify(chargesRepository, Mockito.times(1)).existsById(Mockito.eq(chargeId));

    }


}
