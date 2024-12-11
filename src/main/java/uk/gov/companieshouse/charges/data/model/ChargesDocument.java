package uk.gov.companieshouse.charges.data.model;

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

    private OffsetDateTime deltaAt;

    private Updated updated;

    /**
     * default constructor.
     */
    public ChargesDocument() {
    }

    /**
     * Argument constructor.
     *
     * @param id            id.
     * @param companyNumber company number.
     * @param data          data.
     * @param updated       updated.
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

    public OffsetDateTime getDeltaAt() {
        return deltaAt;
    }

    public ChargesDocument setDeltaAt(OffsetDateTime deltaAt) {
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ChargesDocument that = (ChargesDocument) obj;
        return id.equals(that.id) && companyNumber.equals(that.companyNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyNumber);
    }

    public static class Updated {

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime at;

        private String type;

        private String by;

        public LocalDateTime getAt() {
            return at;
        }

        public Updated setAt(LocalDateTime at) {
            this.at = at;
            return this;
        }

        public String getType() {
            return type;
        }

        public Updated setType(String type) {
            this.type = type;
            return this;
        }

        public String getBy() {
            return by;
        }

        public Updated setBy(String by) {
            this.by = by;
            return this;
        }

    }
}
