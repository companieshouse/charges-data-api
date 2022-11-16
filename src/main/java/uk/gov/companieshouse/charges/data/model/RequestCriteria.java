package uk.gov.companieshouse.charges.data.model;

public class RequestCriteria {
    private Integer itemsPerPage;
    private Integer startIndex;
    private String filter;

    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    public RequestCriteria setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
        return this;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public RequestCriteria setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
        return this;
    }

    public String getFilter() {
        return filter;
    }

    public RequestCriteria setFilter(String filter) {
        this.filter = filter;
        return this;
    }
}
