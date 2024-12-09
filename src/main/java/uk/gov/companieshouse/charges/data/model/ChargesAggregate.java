package uk.gov.companieshouse.charges.data.model;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document
public class ChargesAggregate {

    @Field(value = "total_charges")
    private List<TotalCharges> totalCharges;

    @Field(value = "charges_documents")
    private List<ChargesDocument> chargesDocuments;

    public ChargesAggregate(List<TotalCharges> totalCharges,
                            List<ChargesDocument> chargesDocuments) {
        this.totalCharges = totalCharges;
        this.chargesDocuments = chargesDocuments;
    }

    public ChargesAggregate() {
        this.totalCharges = new ArrayList<>();
        this.chargesDocuments = new ArrayList<>();
    }

    public List<TotalCharges> getTotalCharges() {
        return totalCharges;
    }

    public ChargesAggregate setTotalCharges(List<TotalCharges> totalCharges) {
        this.totalCharges = totalCharges;
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
