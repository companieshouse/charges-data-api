package uk.gov.companieshouse.charges.data.api;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.metrics.request.PrivateCompanyMetricsGet;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;


@Service
public class CompanyMetricsApiService implements ApiClientService {

    private static final String GET_COMPANY_METRICS_ENDPOINT = "/company/%s/metrics";
    private final Logger logger;

    @Value("${api.company.metrics.key}")
    private String companyMetricsApiKey;

    @Value("${api.company.metrics.endpoint}")
    private String companyMetricsApiUrl;

    /**
     * Invoke Company Metrics API.
     */
    @Autowired
    public CompanyMetricsApiService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Get company metrics.
     *
     * @param companyNumber company number.
     * @return company metrics.
     */
    public Optional<MetricsApi> getCompanyMetrics(final String companyNumber) {
        logger.debug(String.format("Started : getCompanyMetrics for Company Number %s ",
                companyNumber
        ));
        final InternalApiClient internalApiClient = getInternalApiClient();
        PrivateCompanyMetricsGet companyMetrics =
                internalApiClient.privateCompanyMetricsResourceHandler()
                        .getCompanyMetrics(
                                String.format(GET_COMPANY_METRICS_ENDPOINT, companyNumber));
        try {
            ApiResponse<MetricsApi> execute = companyMetrics.execute();
            return Optional.ofNullable(execute.getData());
        } catch (URIValidationException exp) {
            logger.error(String.format(
                    "Error occurred while calling /resource-changed endpoint. "
                            + "Message: %s StackTrace: ",
                    exp.getMessage(), exp.getStackTrace().toString()));

        } catch (ApiErrorResponseException exp) {
            logger.error(String.format(
                    "Error occurred while calling /resource-changed endpoint. "
                            + "Message: %s StackTrace: ",
                    exp.getMessage(), exp.getStackTrace().toString()));
            throw new ResponseStatusException(HttpStatus.valueOf(exp.getStatusCode()),
                    exp.getStatusMessage(), exp);
        }
        logger.debug(String.format("Finished : getCompanyMetrics for Company Number %s ",
                companyNumber
        ));
        return Optional.empty();
    }

    /**
     * Get an internal api client instance.
     */
    @Override
    public InternalApiClient getInternalApiClient() {
        InternalApiClient apiClient = new InternalApiClient(getHttpClient());
        apiClient.setBasePath(companyMetricsApiUrl);
        return apiClient;
    }

    private HttpClient getHttpClient() {
        ApiKeyHttpClient httpClient = new ApiKeyHttpClient(companyMetricsApiKey);
        return httpClient;
    }

}
