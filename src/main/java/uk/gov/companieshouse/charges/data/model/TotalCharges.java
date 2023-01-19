package uk.gov.companieshouse.charges.data.model;

public class TotalCharges {

    private Long count;

    public TotalCharges() {
    }

    public TotalCharges(Long count) {
        this.count = count;
    }

    public Long getCount() {
        return count;
    }

    public TotalCharges setCount(Long count) {
        this.count = count;
        return this;
    }
}
