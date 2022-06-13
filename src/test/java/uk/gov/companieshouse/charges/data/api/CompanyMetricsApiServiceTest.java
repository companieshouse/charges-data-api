package uk.gov.companieshouse.charges.data.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.metrics.PrivateCompanyMetricsResourceHandler;
import uk.gov.companieshouse.api.handler.metrics.request.PrivateCompanyMetricsGet;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
public class CompanyMetricsApiServiceTest {

    @Mock
    private ApiClientService apiClientService;

    private final Logger logger = Mockito.mock(Logger.class);;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateCompanyMetricsResourceHandler privateCompanyMetricsResourceHandler;

    @Mock
    private PrivateCompanyMetricsGet privateCompanyMetricsGet;

    @Mock
    private ApiResponse<MetricsApi> response;

    @InjectMocks
    private CompanyMetricsApiService companyMetricsApiService;

    @Test
    void should_invoke_company_metrics_endpoint_successfully()
            throws ApiErrorResponseException, URIValidationException {

        when(apiClientService.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsResourceHandler()).thenReturn(
                privateCompanyMetricsResourceHandler);
        when(privateCompanyMetricsResourceHandler.getCompanyMetrics(Mockito.any())).thenReturn(
                privateCompanyMetricsGet);
        when(privateCompanyMetricsGet.execute()).thenReturn(response);

        Optional<MetricsApi> apiResponse = companyMetricsApiService.getCompanyMetrics("00006400");

        assertThat(apiResponse).isNotNull();
        verify(apiClientService, times(1)).getInternalApiClient();
        verify(privateCompanyMetricsResourceHandler, times(1)).getCompanyMetrics(Mockito.any());
        verify(privateCompanyMetricsGet, times(1)).execute();
    }

    @Test
    void should_handle_exception_when_company_metrics_endpoint_throws_exception()
            throws ApiErrorResponseException, URIValidationException {

        when(apiClientService.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsResourceHandler()).thenReturn(
                privateCompanyMetricsResourceHandler);
        when(privateCompanyMetricsResourceHandler.getCompanyMetrics(Mockito.any())).thenReturn(
                privateCompanyMetricsGet);
        when(privateCompanyMetricsGet.execute()).thenThrow(ApiErrorResponseException.class);
        assertThrows(ResponseStatusException.class, () -> companyMetricsApiService.getCompanyMetrics("00006400"));
        verify(apiClientService, times(1)).getInternalApiClient();
        verify(privateCompanyMetricsResourceHandler, times(1)).getCompanyMetrics(Mockito.any());
        verify(privateCompanyMetricsGet, times(1)).execute();
    }
}
