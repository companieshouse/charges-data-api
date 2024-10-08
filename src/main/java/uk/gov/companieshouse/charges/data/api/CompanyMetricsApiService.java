package uk.gov.companieshouse.charges.data.api;

import static uk.gov.companieshouse.charges.data.ChargesDataApiApplication.NAMESPACE;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.metrics.request.PrivateCompanyMetricsGet;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;


@Service
public class CompanyMetricsApiService {

    private static final String GET_COMPANY_METRICS_ENDPOINT = "/company/%s/metrics";
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);


    private ApiClientService apiClientService;

    /**
     * Invoke Company Metrics API.
     */
    @Autowired
    public CompanyMetricsApiService(@Qualifier("CompanyMetricsApiClient") ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    /**
     * Get company metrics.
     *
     * @param companyNumber company number.
     * @return company metrics.
     */
    public Optional<MetricsApi> getCompanyMetrics(final String companyNumber) {
        LOGGER.info(String.format("Started : getCompanyMetrics for Company Number %s ", companyNumber),
                DataMapHolder.getLogMap());
        final InternalApiClient internalApiClient = this.apiClientService.getInternalApiClient();
        internalApiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        PrivateCompanyMetricsGet companyMetrics =
                internalApiClient.privateCompanyMetricsResourceHandler()
                        .getCompanyMetrics(
                                String.format(GET_COMPANY_METRICS_ENDPOINT, companyNumber));
        try {
            ApiResponse<MetricsApi> execute = companyMetrics.execute();
            LOGGER.debug("Finished : getCompanyMetrics", DataMapHolder.getLogMap());
            return Optional.ofNullable(execute.getData());
        } catch (URIValidationException exp) {
            LOGGER.error("Error occurred while calling getCompanyMetrics endpoint.", DataMapHolder.getLogMap());

        } catch (ApiErrorResponseException exp) {
            if (exp.getStatusCode() == 404) {
                LOGGER.error("Not Found error occurred while calling getCompanyMetrics endpoint.",
                        DataMapHolder.getLogMap());
            } else {
                LOGGER.error("Error when calling getCompanyMetrics endpoint.", DataMapHolder.getLogMap());
                throw new ResponseStatusException(exp.getStatusCode(),
                        exp.getStatusMessage(), null);
            }
        }
        return Optional.empty();
    }
}
