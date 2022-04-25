package uk.gov.companieshouse.charges.data.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.api.CompanyMetricsApiService;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class ChargesServiceTest {

    private final static String companyNumber = "NI622400";

    @Autowired
    private ObjectMapper mongoCustomConversions;

    private static ChargesRepository chargesRepository;

    private static ChargesTransformer chargesTransformer;

    private static ChargesApiService chargesApiService;

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
     * was always returing null because it was a different mock instance.
     *
     * Suspect this was due to sprint injection, not proven but
     * mock are now the same in test and serice to tests work and pass.
     */
    @BeforeAll
    public static void setup(){
        chargesApiService = mock(ChargesApiService.class);
        companyMetricsApiService = mock(CompanyMetricsApiService.class);
        chargesTransformer = mock(ChargesTransformer.class);
        chargesRepository = mock(ChargesRepository.class);
        Logger logger = mock(Logger.class);
        chargesService = new ChargesService(logger, chargesRepository,
            chargesTransformer, chargesApiService, companyMetricsApiService);
    }

    /**
     * Reset the mocks so defaults are returned and invocation counters cleared.
     */
    @BeforeEach
    public void resetMocks(){
        reset(companyMetricsApiService);
        reset(chargesRepository);
    }

    @Test
    public void find_charges_should_return_charges() throws IOException {
        Pageable pageable = Pageable.ofSize(1);
        final PageImpl page = new PageImpl<>(
                List.of(createCharges()));
        when(chargesRepository.findCharges(eq(companyNumber), any(Pageable.class))).thenReturn(page);

        when(companyMetricsApiService.getCompanyMetrics(companyNumber)).thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges.isPresent()).isTrue();
        assertThat(charges.get().getItems().isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
    }

    @Test
    public void empty_charges_when_repository_returns_empty_result() throws IOException {
        var pageable = Pageable.ofSize(1);
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges.isPresent()).isFalse();
        verify(companyMetricsApiService, times(0)).getCompanyMetrics(companyNumber);
    }

    @Test
    public void empty_charges_when_company_metrics_returns_no_result() throws IOException {
        var pageable = Pageable.ofSize(1);
        var page = new PageImpl<>(
                List.of(createCharges()));
        when(chargesRepository.findCharges(eq(companyNumber), any(Pageable.class))).thenReturn(page);

        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges.isPresent()).isFalse();
    }

    private ChargesDocument createCharges() throws
            IOException {
        Document chargesBson = readData(chargesFile);
        ChargesDocument chargesDocument =
                mongoCustomConversions.convertValue(chargesBson, ChargesDocument.class);
        return chargesDocument;
    }

    private MetricsApi createMetrics() throws
            IOException {
        Document chargesBson = readData(metricsFile);
        MetricsApi metricsApi =
                mongoCustomConversions.convertValue(chargesBson, MetricsApi.class);
        return metricsApi;
    }

    private Document readData(Resource resource) throws IOException {
        var data= FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                resource.getInputStream())));
        Document document = Document.parse(data);
        return document;
    }
}
