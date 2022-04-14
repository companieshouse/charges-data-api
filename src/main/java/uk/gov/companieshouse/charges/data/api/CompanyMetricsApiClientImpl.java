package uk.gov.companieshouse.charges.data.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("CompanyMetricsApiClient")
public class CompanyMetricsApiClientImpl extends ApiClientServiceImpl {

    public CompanyMetricsApiClientImpl(
            @Value("${api.company.metrics.key}") String companyMetricsApiKey,
            @Value("${api.company.metrics.endpoint}") String internalApiUrl) {
        super(companyMetricsApiKey, internalApiUrl);
    }

}
