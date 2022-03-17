package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateDeSerializer extends JsonDeserializer<LocalDate> {

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public LocalDate deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        try {
            final LocalDate date =
                    LocalDate.parse(jsonNode.get("$date").textValue(), dateTimeFormatter);
            return date;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
