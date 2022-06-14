package uk.gov.companieshouse.charges.data.api;

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
import uk.gov.companieshouse.logging.Logger;


@Service
public class CompanyMetricsApiService {

    private static final String GET_COMPANY_METRICS_ENDPOINT = "/company/%s/metrics";
    private final Logger logger;


    private ApiClientService apiClientService;

    /**
     * Invoke Company Metrics API.
     */
    @Autowired
    public CompanyMetricsApiService(Logger logger,
            @Qualifier("CompanyMetricsApiClient") ApiClientService apiClientService) {
        this.logger = logger;
        this.apiClientService = apiClientService;
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
        final InternalApiClient internalApiClient = this.apiClientService.getInternalApiClient();
        PrivateCompanyMetricsGet companyMetrics =
                internalApiClient.privateCompanyMetricsResourceHandler()
                        .getCompanyMetrics(
                                String.format(GET_COMPANY_METRICS_ENDPOINT, companyNumber));
        try {
            ApiResponse<MetricsApi> execute = companyMetrics.execute();
            return Optional.ofNullable(execute.getData());
        } catch (URIValidationException exp) {
            logger.error("Error occurred while calling getCompanyMetrics endpoint. ", exp);

        } catch (ApiErrorResponseException exp) {
            if (exp.getStatusCode() == 410) {
                logger.info(String.format(
                        "Error occurred while calling getCompanyMetrics endpoint "
                        + "not found for %s.", companyNumber));
            } else {
                logger.error("Error occurred while calling getCompanyMetrics endpoint. ", exp);
                throw new ResponseStatusException(exp.getStatusCode(),
                    exp.getStatusMessage(), exp);
            }
        }
        logger.debug(String.format("Finished : getCompanyMetrics for Company Number %s ",
                companyNumber
        ));
        return Optional.empty();
    }
}
