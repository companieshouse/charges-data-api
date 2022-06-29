package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import uk.gov.companieshouse.charges.data.config.LoggingConfig;
import uk.gov.companieshouse.charges.data.util.DateTimeFormatter;

public class LocalDateDeSerializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser jsonParser,
                                 DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        try {
            if (JsonNodeType.STRING.equals(jsonNode.getNodeType())) {
                var dateStr = jsonNode.textValue();
                if (!StringUtils.isBlank(dateStr)) {
                    return DateTimeFormatter.parse(dateStr);
                } else {
                    LoggingConfig.getLogger().debug("Ignoring empty date string.");
                    return null;
                }

            } else {
                var dateJsonNode = jsonNode.get("$date");
                if (dateJsonNode == null) {
                    return DateTimeFormatter.parse(jsonNode.textValue());
                } else if (dateJsonNode.isTextual()) {
                    var dateStr = dateJsonNode.textValue();
                    return DateTimeFormatter.parse(dateStr);
                } else {
                    var longDate = dateJsonNode.get("$numberLong").asLong();
                    var dateStr = Instant.ofEpochMilli(new Date(longDate).getTime()).toString();
                    return DateTimeFormatter.parse(dateStr);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed while deserializing "
                    + "date value for json node: %s", jsonNode), ex);
        }
    }
}
