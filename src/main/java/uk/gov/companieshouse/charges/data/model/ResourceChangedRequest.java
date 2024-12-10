package uk.gov.companieshouse.charges.data.model;

import java.util.Objects;
import uk.gov.companieshouse.api.charges.ChargeApi;

public record ResourceChangedRequest(String contextId, String chargeId, String companyNumber, ChargeApi data, Boolean isDelete) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceChangedRequest that = (ResourceChangedRequest) o;
        return Objects.equals(contextId, that.contextId) && Objects.equals(companyNumber,
                that.companyNumber) && Objects.equals(chargeId, that.chargeId) && Objects.equals(data,
                that.data) && Objects.equals(isDelete, that.isDelete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId, companyNumber, chargeId, data, isDelete);
    }
}
