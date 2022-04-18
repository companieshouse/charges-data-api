package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.LocalDate;
import uk.gov.companieshouse.charges.data.util.DateFormatter;

public class LocalDateDeSerializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        try {
            var dateJsonNode = jsonNode.get("$date");
            if (dateJsonNode.isTextual()) {
                String dateStr = dateJsonNode.textValue();
                return DateFormatter.parse(dateStr);
            } else {
                var longDate = dateJsonNode.get("$numberLong").asLong();
                String dateStr = LocalDate.ofEpochDay(longDate).toString();
                return DateFormatter.parse(dateStr);
            }
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed while deserializing "
                            + "date value for json node %s "
                            + "StackTrace: %s  Error Message: %s" ,
                    jsonNode.toPrettyString(),
                    ex.getStackTrace(),
                    ex.getMessage()));
        }
    }
}
