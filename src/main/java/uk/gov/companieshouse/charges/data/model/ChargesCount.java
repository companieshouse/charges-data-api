package uk.gov.companieshouse.charges.data.model;

public class ChargesCount {

    private Long count;

    public ChargesCount() {
    }

    public ChargesCount(Long count) {
        this.count = count;
    }

    public Long getCount() {
        return count;
    }

    public ChargesCount setCount(Long count) {
        this.count = count;
        return this;
    }
}
