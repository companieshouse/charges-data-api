package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import uk.gov.companieshouse.charges.data.util.DateTimeFormatter;

public class LocalDateDeSerializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser jsonParser,
                                 DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        try {
            var dateJsonNode = jsonNode.get("$date");
            if (dateJsonNode.isTextual()) {
                var dateStr = dateJsonNode.textValue();
                return DateTimeFormatter.parse(dateStr);
            } else {
                var longDate = dateJsonNode.get("$numberLong").asLong();
                var dateStr = Instant.ofEpochMilli(new Date(longDate).getTime()).toString();
                return DateTimeFormatter.parse(dateStr);
            }
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed while deserializing "
                    + "date value for json node: %s", jsonNode), ex);
        }
    }
}
