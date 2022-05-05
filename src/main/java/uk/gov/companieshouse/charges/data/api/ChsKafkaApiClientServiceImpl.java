package uk.gov.companieshouse.charges.data.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("ChsKafkaApiClientService")
public class ChsKafkaApiClientServiceImpl extends ApiClientServiceImpl {

    public ChsKafkaApiClientServiceImpl(@Value("${chs.kafka.api.key}") String chsApiKey,
                                        @Value("${chs.kafka.api.endpoint}") String internalApiUrl) {
        super(chsApiKey, internalApiUrl);
    }

}
