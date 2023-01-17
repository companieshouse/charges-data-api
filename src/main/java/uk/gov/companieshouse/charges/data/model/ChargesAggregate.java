package uk.gov.companieshouse.charges.data.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document
public class ChargesAggregate {

    private List<Long> count;

    @Field(value = "charges_documents")
    private List<ChargesDocument> chargesDocuments;

    public ChargesAggregate(List<Long> count, List<ChargesDocument> chargesDocuments) {
        this.count = count;
        this.chargesDocuments = chargesDocuments;
    }

    public ChargesAggregate() {
    }

    public List<Long> getCount() {
        return count;
    }

    public ChargesAggregate setCount(List<Long> count) {
        this.count = count;
        return this;
    }

    public List<ChargesDocument> getChargesDocuments() {
        return chargesDocuments;
    }

    public ChargesAggregate setChargesDocuments(List<ChargesDocument> chargesDocuments) {
        this.chargesDocuments = chargesDocuments;
        return this;
    }
}
