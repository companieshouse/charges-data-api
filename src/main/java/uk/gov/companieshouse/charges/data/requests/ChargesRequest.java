package uk.gov.companieshouse.charges.data.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.companieshouse.api.charges.InternalChargeApi;

public class ChargesRequest {
    @JsonProperty("charges")
    private InternalChargeApi charges;
}
