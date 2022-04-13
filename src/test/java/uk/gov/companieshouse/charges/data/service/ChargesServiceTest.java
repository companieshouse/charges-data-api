package uk.gov.companieshouse.charges.data.service;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private ObjectMapper mongoCustomConversions;

    @Mock
    private Logger logger;
    @Mock
    ChargesRepository chargesRepository;
    @Mock
    ChargesTransformer chargesTransformer;
    @Mock
    ChargesApiService chargesApiService;
    @Mock
    CompanyMetricsApiService companyMetricsApiService;

    @Value("file:src/test/resources/company-metrics-data.json")
    Resource metricsFile;

    @Value("file:src/test/resources/charges-test-DB-record.json")
    Resource chargesFile;

    @InjectMocks
    ChargesService chargesService;

    @Test
    public void find_charges_should_return_charges() throws IOException {
        var companyNumber = "NI622400";
        var pageable = Pageable.ofSize(1);
        var page = new PageImpl<>(
                List.of(createCharges()));
        when(chargesRepository.findCharges(companyNumber, pageable)).thenReturn(page);
        when(companyMetricsApiService.getCompanyMetrics(companyNumber)).thenReturn(Optional.ofNullable(createMetrics()));
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges).isNotNull();
        assertThat(charges.isEmpty()).isFalse();
        assertThat(charges.get().getTotalCount()).isEqualTo(1);
        assertThat(charges.get().getSatisfiedCount()).isEqualTo(1);
        assertThat(charges.get().getPartSatisfiedCount()).isEqualTo(2);
        assertThat(charges.get().getUnfilteredCount()).isEqualTo(14);
    }

    @Test
    public void empty_charges_when_repository_returns_empty_result() throws IOException {
        var companyNumber = "NI622400";
        var pageable = Pageable.ofSize(1);
        when(chargesRepository.findCharges(companyNumber, pageable)).thenReturn(Page.empty());
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges).isNotNull();
        assertThat(charges.isEmpty()).isTrue();
        verify(companyMetricsApiService, times(0)).getCompanyMetrics(companyNumber);
    }

    @Test
    public void empty_charges_when_company_metrics_returns_no_result() throws IOException {
        var companyNumber = "NI622400";
        var pageable = Pageable.ofSize(1);
        var page = new PageImpl<>(
                List.of(createCharges()));
        when(chargesRepository.findCharges(companyNumber, pageable)).thenReturn(page);
        when(companyMetricsApiService.getCompanyMetrics(companyNumber)).thenReturn(Optional.empty());
        Optional<ChargesApi> charges = chargesService.findCharges(companyNumber,pageable);
        assertThat(charges).isNotNull();
        assertThat(charges.isEmpty()).isTrue();
    }

    private ChargesDocument createCharges() throws
            IOException {
        String chargesData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                        chargesFile.getInputStream())));
        Document chargesBson = Document.parse(chargesData);
        ChargesDocument chargesDocument =
                mongoCustomConversions.convertValue(chargesBson, ChargesDocument.class);
        return chargesDocument;
    }

    private MetricsApi createMetrics() throws
            IOException {
        String metricsData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                        metricsFile.getInputStream())));
        Document chargesBson = Document.parse(metricsData);
        MetricsApi metricsApi =
                mongoCustomConversions.convertValue(chargesBson, MetricsApi.class);
        return metricsApi;
    }
}
