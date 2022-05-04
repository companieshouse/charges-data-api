package uk.gov.companieshouse.charges.data.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;
import uk.gov.companieshouse.api.charges.ChargeApi;

@Document(collection = "#{@environment.getProperty('mongodb.charges.collection.name')}")
public class ChargesDocument {

    @Id
    private String id;

    @Field(value = "company_number")
    @Indexed(unique = true)
    private String companyNumber;

    private ChargeApi data;

    @Field("delta_at")
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
    )
    private LocalDateTime deltaAt;

    private Updated updated;

    /**
     * default constructor.
     */
    public ChargesDocument() {

    }

    /**
     * Argument constructor.
     * @param id id.
     * @param companyNumber company number.
     * @param data data.
     * @param updated updated.
     */
    public ChargesDocument(String id, String companyNumber,
            ChargeApi data, Updated updated) {
        this.id = id;
        this.companyNumber = companyNumber;
        this.data = data;
        this.updated = updated;
    }

    public String getId() {
        return id;
    }

    public ChargesDocument setId(String id) {
        this.id = id;
        return this;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public ChargesDocument setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
        return this;
    }

    public ChargeApi getData() {
        return this.data;
    }

    public ChargesDocument setData(ChargeApi data) {
        this.data = data;
        return this;
    }

    public LocalDateTime getDeltaAt() {
        return deltaAt;
    }

    public ChargesDocument setDeltaAt(LocalDateTime deltaAt) {
        this.deltaAt = deltaAt;
        return this;
    }

    public Updated getUpdated() {
        return updated;
    }

    public ChargesDocument setUpdated(Updated updated) {
        this.updated = updated;
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ChargesDocument{");
        sb.append("id=").append(id);
        sb.append(", company_number=").append(companyNumber);
        sb.append(", data=").append(data.toString());
        sb.append(", delta_at=").append(deltaAt.toString());
        sb.append(", updated=").append(updated.toString());

        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ChargesDocument that = (ChargesDocument) obj;
        return id.equals(that.id) && companyNumber.equals(that.companyNumber) && data.equals(
                that.data) && deltaAt.equals(that.deltaAt) && updated.equals(that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyNumber, data, deltaAt, updated);
    }
}
