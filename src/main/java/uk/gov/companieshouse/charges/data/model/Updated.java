package uk.gov.companieshouse.charges.data.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

public class Updated {

    private OffsetDateTime at;
    private String type;
    private String by;


    public OffsetDateTime getAt() {
        return at;
    }

    public Updated setAt(OffsetDateTime at) {
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Updated{");
        sb.append("at=").append(at);
        sb.append(", type=").append(type);
        sb.append(", by=").append(by);
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
        Updated updated = (Updated) obj;
        return at.equals(updated.at) && type.equals(updated.type) && by.equals(updated.by);
    }

    @Override
    public int hashCode() {
        return Objects.hash(at, type, by);
    }
}
