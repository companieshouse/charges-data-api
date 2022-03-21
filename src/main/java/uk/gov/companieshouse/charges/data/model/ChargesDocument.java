package uk.gov.companieshouse.charges.data.model;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.charges.ChargeApi;

@Document(collection = "#{@environment.getProperty('mongodb.charges.collection.name')}")
public class ChargesDocument {

    @Id
    private String id;

    @Field(value = "company_number")
    //TODO Zahid is going to confirm whether we need to index on charges or company_number
    //@Indexed(unique = true)
    private String companyNumber;

    private ChargeApi data;

    private Updated updated;

    @Version
    private Long version;

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
        sb.append(", updated=").append(updated.toString());
        sb.append(", version=").append(version.toString());
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
                that.data)
                && updated.equals(that.updated)
                && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyNumber, data, updated, version);
    }
}
