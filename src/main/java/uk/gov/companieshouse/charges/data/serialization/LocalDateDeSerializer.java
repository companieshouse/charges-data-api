package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.core.JacksonException;
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
            DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        try {
            String dateStr = jsonNode.get("$date").textValue();
            final LocalDate date = DateFormatter.parse(dateStr);
            return date;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
